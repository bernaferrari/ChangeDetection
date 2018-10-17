package com.bernaferrari.changedetection

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.work.State
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.SiteAndLastSnap
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.forms.FormInputText
import com.bernaferrari.changedetection.forms.Forms
import com.bernaferrari.changedetection.groupie.*
import com.bernaferrari.changedetection.ui.InsetDecoration
import com.bernaferrari.changedetection.ui.ListPaddingDecoration
import com.bernaferrari.changedetection.util.GradientColors
import com.bernaferrari.changedetection.util.GradientColors.getGradientDrawable
import com.bernaferrari.changedetection.util.LongPress
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.tapadoo.alerter.Alerter
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.state_layout.*
import kotlinx.android.synthetic.main.state_layout.view.*
import kotlinx.coroutines.experimental.*

class MainFragment : ScopedFragment() {
    private lateinit var mViewModel: MainViewModel
    private val sitesList = mutableListOf<MainCardItem>()
    private val sitesSection = Section(sitesList)

    private val transitionDelay = 125L
    private val transition = AutoTransition().apply { duration = transitionDelay }

    private lateinit var filterItem: MenuItem
    private val isDarkModeOn = Injector.get().sharedPrefs().getBoolean(MainActivity.DARKMODE, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        WorkerHelper.updateWorkerWithConstraints(Injector.get().sharedPrefs())
        WorkManager.getInstance().pruneWork()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.main_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.setSupportActionBar(toolbar)

        mViewModel = viewModelProvider(ViewModelFactory.getInstance(requireActivity().application))

        mViewModel.sortAlphabetically = Injector.get().sharedPrefs().getBoolean("sortByName", false)

        stateLayout.showLoading()

        fab.setOnClickListener { showCreateEditDialog(false) }

        pullToRefresh.setOnRefreshListener {
            sitesList.forEach(this::reloadEach)
            pullToRefresh.isRefreshing = false
        }

        val groupAdapter = GroupAdapter<ViewHolder>().apply {
            add(sitesSection)
        }

        defaultRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = itemAnimatorWithoutChangeAnimations()
            addItemDecoration(ListPaddingDecoration(context))
            adapter = groupAdapter

            setEmptyView(view.stateLayout.apply {
                setEmptyText(getString(R.string.no_websites_being_monitored))
            })
        }

        groupAdapter.setOnItemClickListener { item, _ ->
            if (item is MainCardItem) openItem(item)
        }

        groupAdapter.setOnItemLongClickListener { item, _ ->
            if (item is MainCardItem) consume { showDialogWithOptions(item) } else false
        }

        mViewModel.loadSites().observe(this, Observer(::updateList))
        mViewModel.getOutputStatus.observe(this, Observer(::workOutput))
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.main, menu)

        filterItem = menu.findItem(R.id.filter).also {
            it.isVisible = sitesList.isNotEmpty()
        }

        menu.findItem(R.id.dark_mode).title = isDarkModeOn.takeIf { it == true }
            ?.let { getString(R.string.disable_dark_mode) }
                ?: getString(R.string.enable_dark_mode)

        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.filter -> onFilterTapped()
            R.id.settings -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .add(SettingsFragment(), "settings").commit()
            }
            R.id.about -> {
                view?.findNavController()?.navigate(R.id.action_mainFragment_to_aboutFragment)
            }
            R.id.dark_mode -> {
                Injector.get().sharedPrefs().also {
                    val value = it.getBoolean(MainActivity.DARKMODE, false)
                    it.edit(true) { putBoolean(MainActivity.DARKMODE, !value) }
                }

                requireActivity().recreate()
            }
        }
        return true
    }

    private fun onFilterTapped() {
        if (filterRecycler.adapter == null) {
            filterRecycler.layoutManager = LinearLayoutManager(this.context)

            var filteredColors = listOf<ColorGroup>()

            // return original list if empty, or the filtered one
            fun filterAndScroll() {
                sitesSection.update(filteredColors.takeIf { it.isEmpty() }?.let { sitesList }
                        ?: sitesList.filter { filteredColors.contains(it.site.colors) })

                defaultRecycler.smoothScrollToPosition(0)
            }

            filterRecycler.adapter = GroupAdapter<ViewHolder>().apply {
                add(
                    DialogItemSwitch(
                        getString(R.string.sort_by_name),
                        IconicsDrawable(context, CommunityMaterial.Icon.cmd_sort_alphabetical)
                            .color(requireContext().getColorFromAttr(R.attr.iconColor)),
                        mViewModel.sortAlphabetically
                    ) {
                        mViewModel.sortAlphabetically = it.isSwitchOn
                        sortList()
                        filterAndScroll()
                        Injector.get().sharedPrefs()
                            .edit { putBoolean("sortByName", mViewModel.sortAlphabetically) }
                    }
                )

                val availableColors =
                    sitesList.asSequence().map { it.site.colors }.distinct().toList()

                add(
                    ColorFilterRecyclerViewItem(availableColors) { pairsList ->
                        filteredColors = pairsList
                        filterAndScroll()
                    }
                )
            }
        }

        defaultRecycler.stopScroll()
        TransitionManager.beginDelayedTransition(parentLayout, transition)
        filterRecycler.isVisible = !filterRecycler.isVisible
        filterItem.icon = ContextCompat.getDrawable(
            requireContext(),
            if (!filterRecycler.isVisible) R.drawable.ic_filter else R.drawable.ic_check
        )

        // need to do this on animation to avoid RecyclerView crashing when
        // “scrapped or attached views may not be recycled”
        filterItem.isEnabled = false
        defaultRecycler.setOnTouchListener { _, _ -> true }

        launch(Dispatchers.Main) {
            delay(transitionDelay + 50)
            filterItem.isEnabled = true
            defaultRecycler.setOnTouchListener { _, _ -> false }
        }
    }


    private fun openItem(item: MainCardItem) {
        val customView = parentLayout.inflate(R.layout.recyclerview)

        val bottomSheet = customView.createBottomSheet()

        val bottomSheetAdapter = GroupAdapter<ViewHolder>().apply {
            add(LoadingItem())
        }

        customView.findViewById<RecyclerView>(R.id.defaultRecycler)
            .adapter = bottomSheetAdapter

        launch {
            updateBottomSheet(item, bottomSheetAdapter, bottomSheet)
        }
    }

    private suspend fun updateBottomSheet(
        item: MainCardItem,
        bottomSheetAdapter: GroupAdapter<ViewHolder>,
        bottomSheet: com.google.android.material.bottomsheet.BottomSheetDialog
    ): Unit = withContext(Dispatchers.Main) {

        val contentTypes = mViewModel.getRecentContentTypes(item.site.id)
        val selectedType = item.lastSnap?.contentType

        bottomSheetAdapter.clear()

        when {
            contentTypes.size <= 1 && contentTypes.firstOrNull()?.count?.let { it > 1 } != true -> {
                bottomSheet.show()
                bottomSheetAdapter.add(EmptyItem(item.site.colors.second))
            }
            contentTypes.size == 1 -> {
                navigateTo(selectedType, item)
            }
            else -> {
                bottomSheet.show()

                val remove: ((String) -> (Unit)) = {
                    MaterialDialog(requireContext())
                        .title(R.string.remove)
                        .message(R.string.remove_content)
                        .positiveButton(R.string.yes) { _ ->
                            GlobalScope.launch {
                                mViewModel.removeSnapsByType(item.site.id, it)
                                updateBottomSheet(item, bottomSheetAdapter, bottomSheet)
                            }
                        }
                        .negativeButton(R.string.no)
                        .show()
                }

                contentTypes.forEach {
                    bottomSheetAdapter.add(
                        ItemContentType(
                            it.contentType,
                            it.count,
                            remove
                        ) { selected ->
                            bottomSheet.dismiss()
                            if (it.count > 1) {
                                navigateTo(selected, item)
                            } else {
                                view?.also { v ->
                                    com.google.android.material.snackbar.Snackbar.make(
                                        v,
                                        R.string.less_than_two,
                                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                                    )
                                        .show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun navigateTo(selectedType: String?, item: MainCardItem) {
        val bundle = bundleOf(
            MainActivity.SITEID to item.site.id,
            MainActivity.TITLE to item.site.title,
            MainActivity.URL to item.site.url,
            MainActivity.TYPE to selectedType,
            MainActivity.LASTCHANGE to item.lastSnap?.timestamp?.convertTimestampToDate()
        )

        val extras = view?.findViewWithTag<View>(item.site.id)?.let {
            FragmentNavigatorExtras(it to getString(R.string.shared_transition))
        }

        val destination = when {
            selectedType == "application/pdf" -> R.id.action_mainFragment_to_pdfFragment
            selectedType?.contains("image") == true -> R.id.action_mainFragment_to_imageCarouselFragment
            else -> R.id.action_mainFragment_to_textFragment
        }

        view?.findNavController()?.navigate(destination, bundle, null, extras)
    }

    private fun showDialogWithOptions(item: MainCardItem) {

        val customView = parentLayout.inflate(R.layout.recyclerview)

        val bottomSheet = customView.createAndShowBottomSheet()

        val dialogItems = mViewModel.generateLongPressList(requireContext(), item)

        val recycler =
            customView.findViewById<RecyclerView>(R.id.defaultRecycler)

        recycler?.addItemDecoration(
            InsetDecoration(
                    resources.getDimensionPixelSize(R.dimen.divider_height),
                    resources.getDimensionPixelSize(R.dimen.long_press_separator_margin),
                ContextCompat.getColor(requireContext(), R.color.separator_color)
                )
            )

        recycler?.adapter = GroupAdapter<ViewHolder>().apply {
            add(Section(dialogItems))

            setOnItemClickListener { dialogItem, _ ->
                if (dialogItem !is DialogItemSimple) return@setOnItemClickListener

                when (dialogItem.kind) {
                    LongPress.EDIT -> showCreateEditDialog(
                        true,
                        item as? MainCardItem
                    )
                    LongPress.OPEN_BROWSER -> {
                        requireContext().openInBrowser(item.site.url)
                    }
                    LongPress.IS_SYNC_ENABLED -> {
                        item.site.copy(isSyncEnabled = !item.site.isSyncEnabled).also {
                            mViewModel.updateSite(it)
                            item.update(it)
                            sortList()
                        }
                    }
                    LongPress.IS_NOTIFICATION_ENABLED -> {
                        item.site.copy(isNotificationEnabled = !item.site.isNotificationEnabled)
                            .also {
                                mViewModel.updateSite(it)
                                item.update(it)
                                sortList()
                            }
                    }
                    LongPress.REMOVE -> removeDialog(item)
                    else -> Unit
                }
                bottomSheet.dismiss()
            }
        }
    }

    private fun workOutput(it: List<WorkStatus>?) {
        val result = it ?: return

//        if (Injector.get().sharedPrefs().getBoolean("debug", false)) {
//            val sb = StringBuilder()
//            result.forEach { sb.append("${it.state.name}\n") }
//            if (sb.isNotEmpty()) {
//                sb.setLength(sb.length - 1) // Remove the last \n from the string
//            }
//            Logger.d("workerLog: $sb")
//            Snackbar.make(view!!, sb, Snackbar.LENGTH_SHORT).show()
//        }

        if (result.any { it.state == State.SUCCEEDED }) {
            mViewModel.updateItems()
            Logger.d("Just refreshed")
        }
    }

    private val reloadCallback = { item: MainCardItem ->
        reload(item, true)
    }

    private fun updateList(mutable: MutableList<SiteAndLastSnap>?) {
        if (mutable == null) return

        stateLayout?.showEmptyState()
        defaultRecycler.updateEmptyView()

        if (mutable.isEmpty()) {
            showCreateEditDialog(false)
        }

        if (sitesList.isNotEmpty()) {
            //Verifies if list is not empty and add values that are not there. Basically, makes a snap.
            mutable.forEach { siteAndLastSnap ->
                // if item from new list is currently on the list, update it. Else, add.
                sitesList.find { cardItem -> cardItem.site.id == siteAndLastSnap.site.id }.also {
                    if (it == null) {
                        sitesList += MainCardItem(
                            siteAndLastSnap.site,
                            siteAndLastSnap.snap,
                            reloadCallback
                        )
                    } else {
                        it.update(siteAndLastSnap.site, siteAndLastSnap.snap)
                    }
                }
            }
        } else {
            mutable.mapTo(sitesList) { MainCardItem(it.site, it.snap, reloadCallback) }
        }

        sitesList.sortByDescending { it.lastSnap?.timestamp }
        sitesSection.update(sitesList)
        sortList()

        // This will be used to automatically sync when app open. Since the variable is on ViewModel,
        // even if we navigate between the app, come back and this fragment's onCreate is called again,
        // the variable will not change.
        if (mViewModel.shouldSyncWhenAppOpen) {
            sitesList.forEach(this::reloadEach)
            mViewModel.shouldSyncWhenAppOpen = false
        }

        activity?.invalidateOptionsMenu()
    }

    private fun reloadEach(item: MainCardItem?) {
        reload(item, false)
    }

    private fun reload(item: MainCardItem?, force: Boolean = false) {
        if (item !is MainCardItem || (!item.site.isSyncEnabled && !force)) return

        item.startSyncing()

        launch(Dispatchers.Main) {
            val (contentTypeCharset, content) = WorkerHelper.fetchFromServer(item.site)
            updateSiteAndSnap(contentTypeCharset, content, item)
        }
    }

    private fun updateSiteAndSnap(
        contentTypeCharset: String,
        content: ByteArray,
        item: MainCardItem
    ) {
        Logger.d("count size -> ${content.size}")

        val newSite = item.site.copy(
            timestamp = System.currentTimeMillis(),
            isSuccessful = content.isNotEmpty()
        )
        mViewModel.updateSite(newSite)

        // text/html;charset=UTF-8 needs to become text/html and UTF-8
        val snap = Snap(
            siteId = item.site.id,
            timestamp = newSite.timestamp,
            contentType = contentTypeCharset.split(";").first(),
            contentCharset = contentTypeCharset.findCharset(),
            contentSize = content.size
        )

        mViewModel.saveWebsite(snap, content).observe(this, Observer { isSuccess ->
            item.update(newSite)

            if (isSuccess != true) {
                return@Observer
            }

            Logger.d("snapId: " + snap.snapId)

            // Only show this toast when there was a change, which means, not on the first sync.
            if (item.lastSnap != null && activity != null) {
                Alerter.create(requireActivity())
                    .setTitle(
                        getString(
                            R.string.was_updated,
                            newSite.title?.takeIf { it.isNotBlank() } ?: newSite.url
                        )
                    )
                    .setBackgroundDrawable(
                        getGradientDrawable(newSite.colors.first, newSite.colors.second)
                    )
                    .setIcon(R.drawable.ic_notification)
                    .show()
                    .also { alert ->
                        alert?.setOnClickListener {
                            openItem(item)
                            alert.hide()
                        }
                    }
            }

            item.update(snap)
            sortList()
        })
    }

    private fun sortList() {
        if (mViewModel.sortAlphabetically) {
            sitesList.sortBy { it.site.title }
        } else {
            // sortByStatus by active/inactive, then by timestamp of the last snapshot, then by item title, and if they are still the same, by the url
            sitesList.sortWith(compareByDescending<MainCardItem> { it.site.isSyncEnabled }.thenByDescending { it.lastSnap?.timestamp }.thenBy { it.site.title }.thenBy { it.site.url })
        }
        sitesSection.update(sitesList)
    }

    private fun removeDialog(item: MainCardItem) {

        val customView = parentLayout.inflate(R.layout.recyclerview)
        val bottomSheet = customView.createAndShowBottomSheet()

        val list = mViewModel.getPruningList(requireContext())

        customView.findViewById<RecyclerView>(R.id.defaultRecycler)
            ?.adapter =
                GroupAdapter<ViewHolder>().apply {
                    add(Section(list))

                    setOnItemClickListener { dialogItem, _ ->
                        if (dialogItem is DialogItemSimple) {
                            when (dialogItem.kind) {
                                LongPress.PRUNING -> mViewModel.pruneSite(item.site.id)
                                LongPress.ALL -> removeItem(item)
                                else -> Unit
                            }
                            bottomSheet.dismiss()
                        }
                    }
                }
    }

    private fun removeItem(item: MainCardItem) {
        sitesList.remove(item)
        sitesSection.update(sitesList)
        mViewModel.removeSite(item.site)
    }

    private fun urlFromClipboardOrEmpty(isItemNull: Boolean): String {
        return isItemNull.takeUnless { false }
            .let { requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }
            ?.let { it.primaryClip?.getItemAt(0) }
            ?.let { it.text?.toString() }
            ?.let { if (it.isNotBlank() && it.isValidUrl()) it else "" } ?: ""
    }

    private fun showCreateEditDialog(
        isInEditingMode: Boolean,
        item: MainCardItem? = null
    ) {
        // Gets the clipboard
        val defaultUrl = urlFromClipboardOrEmpty(item == null)

        val listOfItems = mutableListOf<FormInputText>().apply {
            add(FormInputText(item?.site?.title ?: "", getString(R.string.title), Forms.NAME))
            add(FormInputText(item?.site?.url ?: defaultUrl, getString(R.string.url), Forms.URL))
        }

        val colorsList = GradientColors.gradients

        val selectedColor = item?.site?.colors ?: colorsList.first()

        val dialogItemTitle = when (isInEditingMode) {
            true -> DialogItemTitle(
                getString(R.string.edittitle),
                getString(R.string.editsubtitle),
                selectedColor
            )
            false ->
                DialogItemTitle(
                    getString(R.string.addtitle),
                    getString(R.string.addsubtitle),
                    selectedColor
                )
        }

        val dialogItemColorPicker =
            ColorPickerRecyclerViewItem(selectedColor, colorsList) {
                dialogItemTitle.gradientColors = it
                dialogItemTitle.notifyChanged()
            }

        val materialDialog = MaterialDialog(requireContext())
            .customView(R.layout.recyclerview, noVerticalPadding = true)
            .noAutoDismiss() // we need this for wiggle/shake effect, else it would dismiss
            .negativeButton(R.string.cancel) { it.dismiss() }
            .positiveButton(R.string.save) { dialog ->
                // This was adapted from an app which was using NoSql. Not the best syntax, but
                // can be adapted for any scenario, kind of a
                // Eureka (https://github.com/xmartlabs/Eureka) for Android.
                val fromForm = Forms.saveData(listOfItems)
                val newTitle = fromForm[Forms.NAME] as? String ?: ""
                val potentialUrl = fromForm[Forms.URL] as? String ?: ""

                if (isInEditingMode && item != null) {
                    if (isUrlWrong(potentialUrl, listOfItems)) {
                        return@positiveButton
                    }

                    val previousUrl = item.site.url

                    val updatedSite = item.site.copy(
                        title = newTitle,
                        url = potentialUrl,
                        colors = dialogItemTitle.gradientColors
                    )

                    // Update internally, i.e. what the user doesn't see
                    mViewModel.updateSite(updatedSite)

                    // Update visually, i.e. what the user see
                    item.update(updatedSite)

                    // Only reload if the url has changed.
                    if (potentialUrl != previousUrl) {
                        reload(item, true)
                    }
                } else {
                    // Some people will forget to put the http:// on the url, so this is going to help them.
                    // This is going to be absolutely sure the current url is invalid, before adding http:// before it.
                    val url = if (!potentialUrl.isValidUrl()) {
                        "http://$potentialUrl"
                    } else {
                        potentialUrl
                    }

                    // If even after this it is still invalid, we wiggle
                    if (isUrlWrong(url, listOfItems)) {
                        return@positiveButton
                    }

                    val site = Site(
                        newTitle,
                        url,
                        System.currentTimeMillis(),
                        dialogItemTitle.gradientColors
                    )

                    mViewModel.saveSite(site)
                    // add and sortByStatus the card
                    val newItem = MainCardItem(site, null, reloadCallback)
                    sitesList += newItem
                    sitesSection.update(sitesList)
                    // Scroll down, so user can see there is a new item.
                    defaultRecycler.smoothScrollToPosition(sitesList.size - 1)
                    reload(newItem, true)
                }
                dialog.dismiss()

                val sharedPrefs = Injector.get().sharedPrefs()
                // when list size is 1 or 2, warn the user that background sync is off
                if (!isInEditingMode && sitesList.size < 3 && !sharedPrefs.getBoolean(
                        "backgroundSync",
                        false
                    )
                ) {
                    MaterialDialog(requireContext())
                        .title(R.string.turn_on_background_sync_title)
                        .message(R.string.turn_on_background_sync_content)
                        .negativeButton(R.string.no)
                        .positiveButton(R.string.yes) {
                            sharedPrefs.edit { putBoolean("backgroundSync", true) }
                            WorkerHelper.updateWorkerWithConstraints(sharedPrefs)
                        }
                        .show()
                }
            }

        materialDialog.getCustomView()
            ?.findViewById<RecyclerView>(R.id.defaultRecycler)?.apply {
            this.overScrollMode = View.OVER_SCROLL_NEVER
            this.layoutManager = LinearLayoutManager(this.context)
            this.addItemDecoration(
                DividerItemDecoration(
                    this.context,
                    DividerItemDecoration.VERTICAL
                )
            )
            this.adapter = GroupAdapter<ViewHolder>().apply {
                add(dialogItemTitle)
                add(Section(listOfItems))
                add(dialogItemColorPicker)
            }
        }

        materialDialog.show()
    }

    private fun isUrlWrong(url: String, listOfItems: MutableList<FormInputText>): Boolean {
        if (!url.isValidUrl()) {
            listOfItems.first { it.kind == Forms.URL }.shakeIt()
            Toast.makeText(this.context, R.string.incorrect_url, Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }
}

