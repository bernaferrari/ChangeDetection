package com.bernaferrari.changedetection

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.work.State
import androidx.work.WorkStatus
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.SiteAndLastSnap
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.extensions.ColorGroup
import com.bernaferrari.changedetection.extensions.findCharset
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.extensions.viewModelProvider
import com.bernaferrari.changedetection.forms.FormInputText
import com.bernaferrari.changedetection.forms.Forms
import com.bernaferrari.changedetection.groupie.*
import com.bernaferrari.changedetection.ui.ListPaddingDecoration
import com.bernaferrari.changedetection.util.GradientColors
import com.bernaferrari.changedetection.util.GradientColors.getGradientDrawable
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.tapadoo.alerter.Alerter
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import es.dmoral.toasty.Toasty
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment.view.*
import kotlinx.android.synthetic.main.state_layout.*
import kotlinx.android.synthetic.main.state_layout.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import java.util.concurrent.TimeUnit

class MainFragment : Fragment() {
    private lateinit var mViewModel: MainViewModel
    private var sitesList = mutableListOf<MainCardItem>()
    private var sitesSection = Section(sitesList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkerHelper.updateWorkerWithConstraints(Injector.get().sharedPrefs())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.main_fragment, container, false)

    private val transitionDelay = 125L
    private val transition = AutoTransition().apply { duration = transitionDelay }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = viewModelProvider(ViewModelFactory.getInstance(requireActivity().application))

        mViewModel.sortAlphabetically = Injector.get().sharedPrefs().getBoolean("sortByName", false)

        val groupAdapter = GroupAdapter<ViewHolder>()

        // Clear it up, just in case of rotation.
        sitesSection.update(sitesList.apply { clear() })

        stateLayout.showLoading()

        settings.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .add(SettingsFragment(), "settings").commit()
        }

        info.setOnClickListener {
            Navigation.findNavController(view)
                .navigate(R.id.action_mainFragment_to_aboutFragment)
        }

        filter.setOnClickListener { _ ->

            if (filterRecycler.adapter == null) {
                filterRecycler.layoutManager = LinearLayoutManager(this.context)

                val color = ContextCompat.getColor(requireActivity(), R.color.FontStrong)
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
                                .color(color),
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
            filter.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    if (!filterRecycler.isVisible) R.drawable.ic_filter else R.drawable.ic_check
                )
            )

            // need to do this on animation to avoid RecyclerView crashing when
            // “scrapped or attached views may not be recycled”
            filter.isEnabled = false
            Completable.timer(
                transitionDelay,
                TimeUnit.MILLISECONDS,
                AndroidSchedulers.mainThread()
            ).subscribe {
                filter.isEnabled = true
            }
        }

        fab.run {
            background = IconicsDrawable(requireActivity(), CommunityMaterial.Icon.cmd_plus)
            setOnClickListener { showCreateEditDialog(false, requireActivity()) }
        }

        pullToRefresh.setOnRefreshListener {
            sitesList.forEach(this::reloadEach)
            pullToRefresh.isRefreshing = false
        }

        defaultRecycler.run {
            addItemDecoration(ListPaddingDecoration(this.context))
            itemAnimator = this.itemAnimator.apply {
                // From https://stackoverflow.com/a/33302517/4418073
                if (this is SimpleItemAnimator) {
                    this.supportsChangeAnimations = false
                }
            }

            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter.apply {
                // to be used when AndroidX becomes a reality and our top bar is replaced with a bottom bar.
                // this.add(MarqueeItem("Change Detection"))
                this.add(sitesSection)
            }

            setEmptyView(view.stateLayout.apply {
                setEmptyText(context.getString(R.string.no_websites_being_monitored))
            })
        }

        groupAdapter.setOnItemLongClickListener { item, _ ->
            return@setOnItemLongClickListener if (item is MainCardItem) {
                showDialogWithOptions(item)
                true
            } else {
                false
            }
        }

        groupAdapter.setOnItemClickListener { item, _ ->
            if (item !is MainCardItem) return@setOnItemClickListener
            openItem(item)
        }

        mViewModel.loadSites().observe(this, Observer(::updateList))
        mViewModel.getOutputStatus.observe(this, Observer(::workOutput))
    }


    private fun openItem(item: MainCardItem) {
        val customView = layoutInflater.inflate(R.layout.recyclerview, view?.parentLayout, false)

        val bottomSheet = BottomSheetDialog(requireContext()).apply {
            setContentView(customView)
            show()
        }

        val bottomSheetAdapter = GroupAdapter<ViewHolder>().apply {
            add(LoadingItem())
        }

        customView.findViewById<RecyclerView>(R.id.defaultRecycler).run {
            adapter = bottomSheetAdapter
        }

        launch {
            updateBottomSheet(item, bottomSheetAdapter, bottomSheet)
        }
    }

    private suspend fun updateBottomSheet(
        item: MainCardItem,
        bottomSheetAdapter: GroupAdapter<ViewHolder>,
        bottomSheet: BottomSheetDialog
    ): Unit = withContext(CommonPool) {

        val contentTypes = mViewModel.getRecentContentTypes(item.site.id)

        val selectedType = item.lastSnap?.contentType

        withContext(UI) {
            bottomSheetAdapter.clear()

            when {
                contentTypes.size <= 1 && contentTypes.firstOrNull()?.count?.let { it > 1 } != true -> {
                    bottomSheetAdapter.add(EmptyItem(item.site.colors.second))
                }
                contentTypes.size == 1 -> {
                    bottomSheet.dismiss()
                    navigateTo(selectedType, item)
                }
                else -> {
                    val remove: ((String) -> (Unit)) = {
                        MaterialDialog.Builder(requireContext())
                            .title(R.string.remove)
                            .content(R.string.remove_content)
                            .positiveText(R.string.yes)
                            .negativeText(R.string.no)
                            .onPositive { _, _ ->
                                launch {
                                    mViewModel.removeSnapsByType(item.site.id, it)
                                    updateBottomSheet(item, bottomSheetAdapter, bottomSheet)
                                }
                            }
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
                                navigateTo(selected, item)
                            })
                    }
                }
            }
        }
    }

    private fun navigateTo(selectedType: String?, item: MainCardItem) {
        val bundle = bundleOf(
            "SITEID" to item.site.id,
            "TITLE" to item.site.title,
            "URL" to item.site.url,
            "TYPE" to selectedType
        )

        view?.let { view ->
            when {
                selectedType == "application/pdf" -> Navigation.findNavController(view)
                    .navigate(R.id.action_mainFragment_to_pdfFragment, bundle)
                selectedType?.contains("image") == true -> Navigation.findNavController(view)
                    .navigate(R.id.action_mainFragment_to_imageCarouselFragment, bundle)
                else -> Navigation.findNavController(view)
                    .navigate(R.id.action_mainFragment_to_openFragment, bundle)
            }
        }
    }

    private fun showDialogWithOptions(item: MainCardItem) {
        val context = requireActivity()
        val color = item.site.colors.second

        val customView =
            layoutInflater.inflate(R.layout.recyclerview, view?.parentLayout, false)
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.setContentView(customView)
        bottomSheet.show()

        val chart = Section()
        val updating = mutableListOf<DialogItemSimple>()

        updating += DialogItemSimple(
            getString(R.string.edit),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_pencil).color(color),
            "edit"
        )

        updating += DialogItemSimple(
            getString(R.string.open_in_browser),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_google_chrome).color(color),
            "openInBrowser"
        )

        // if item is disabled, makes no sense to enable/disable the notifications
        if (item.site.isSyncEnabled) {
            updating += DialogItemSimple(
                item.site.isNotificationEnabled
                    .takeIf { it == true }
                    ?.let { getString(R.string.notification_disable) }
                        ?: getString(R.string.notification_enable),
                IconicsDrawable(
                    context,
                    item.site.isNotificationEnabled
                        .takeIf { it == true }
                        ?.let { CommunityMaterial.Icon.cmd_bell_off }
                            ?: CommunityMaterial.Icon.cmd_bell)
                    .color(color),
                "isNotificationEnabled"
            )
        }

        updating += DialogItemSimple(
            item.site.isSyncEnabled
                .takeIf { it == true }
                ?.let { getString(R.string.sync_disable) } ?: getString(R.string.sync_enable),
            IconicsDrawable(
                context,
                item.site.isSyncEnabled
                    .takeIf { it == true }
                    ?.let { CommunityMaterial.Icon.cmd_sync_off }
                        ?: CommunityMaterial.Icon.cmd_sync)
                .color(color),
            "isSyncEnabled"
        )

        updating += DialogItemSimple(
            getString(R.string.remove_more),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_delete).color(color),
            "remove"
        )

        customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {

            this.addItemDecoration(
                com.bernaferrari.changedetection.ui.InsetDecoration(
                    resources.getDimensionPixelSize(R.dimen.divider_height),
                    resources.getDimensionPixelSize(R.dimen.long_press_separator_margin),
                    ContextCompat.getColor(this.context, R.color.CCC)
                )
            )

            adapter = GroupAdapter<ViewHolder>().apply {
                add(chart)
                add(Section(updating))

                setOnItemClickListener { dialogitem, _ ->
                    if (dialogitem !is DialogItemSimple) return@setOnItemClickListener

                    when (dialogitem.kind) {
                        "edit" -> showCreateEditDialog(
                            true,
                            context,
                            item as? MainCardItem
                        )
                        "openInBrowser" -> {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.site.url)))
                        }
                        "isSyncEnabled" -> {
                            item.site.copy(isSyncEnabled = !item.site.isSyncEnabled).also {
                                mViewModel.updateSite(it)
                                item.update(it)
                                sortList()
                            }
                        }
                        "isNotificationEnabled" -> {
                            item.site.copy(isNotificationEnabled = !item.site.isNotificationEnabled)
                                .also {
                                    mViewModel.updateSite(it)
                                    item.update(it)
                                    sortList()
                                }
                        }
                        "fetchFromServer" -> reload(item, true)
                        "remove" -> removeDialog(item)
                    }
                    bottomSheet.dismiss()
                }
            }
        }
    }

    private fun workOutput(it: List<WorkStatus>?) {
        val result = it ?: return
        val sb = StringBuilder()
        result.forEach { sb.append("${it.id}: ${it.state.name}\n") }
        if (sb.isNotEmpty()) {
            sb.setLength(sb.length - 1) // Remove the last \n from the string
        }
        if (result.firstOrNull()?.state == State.SUCCEEDED) {
            mViewModel.updateItems()
            Logger.d("Just refreshed")
        }
    }

    private val reloadCallback = { item: MainCardItem ->
        reload(item, true)
    }

    private fun updateList(mutable: MutableList<SiteAndLastSnap>?) {
        if (mutable == null) {
            return
        }

        view?.stateLayout?.showEmptyState()

        if (mutable.isEmpty()) {
            showCreateEditDialog(false, requireActivity())
        }

        if (sitesList.isNotEmpty()) {
            //Verifies if list is not empty and add values that are not there. Basically, makes a snap.
            mutable.forEach { siteAndLastSnap ->
                // if item from new list is currently on the list, update it. Else, add.
                sitesList.find { carditem -> carditem.site.id == siteAndLastSnap.site.id }.also {
                    if (it == null) {
                        sitesList.add(
                            MainCardItem(
                                siteAndLastSnap.site,
                                siteAndLastSnap.snap,
                                reloadCallback
                            )
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

        if (sitesList.isNotEmpty()) filter.isVisible = true
    }

    private fun reloadEach(item: MainCardItem?) {
        reload(item, false)
    }

    private fun reload(item: MainCardItem?, force: Boolean = false) {
        if (item !is MainCardItem || (!item.site.isSyncEnabled && !force)) {
            return
        }

        item.startSyncing()
        launch {
            val strFetched = WorkerHelper.fetchFromServer(item.site)
            launch(UI) { updateSiteAndSnap(strFetched.first, strFetched.second, item) }
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

        val customView = layoutInflater.inflate(R.layout.recyclerview, view?.parentLayout, false)
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.setContentView(customView)
        bottomSheet.show()

        val updating = mutableListOf<DialogItemSimple>()
        val color = ContextCompat.getColor(requireContext(), R.color.FontStrong)

        updating += DialogItemSimple(
            getString(R.string.pruning),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_content_cut).color(color),
            "pruning"
        )

        updating += DialogItemSimple(
            getString(R.string.remove_all),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_delete).color(color),
            "all"
        )

        customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {

            adapter = GroupAdapter<ViewHolder>().apply {
                add(Section(updating))

                setOnItemClickListener { dialogitem, _ ->
                    if (dialogitem !is DialogItemSimple) return@setOnItemClickListener

                    when (dialogitem.kind) {
                        "pruning" -> mViewModel.pruneSite(item.site.id)
                        "all" -> removeItem(item)
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
        return if (isItemNull) {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        ?: return ""

            val clipDataItem = clipboard.primaryClip?.getItemAt(0) ?: return ""
            val pasteData = clipDataItem.text?.toString() ?: ""

            if (pasteData.isNotBlank() && pasteData.isValidUrl()) {
                pasteData
            } else {
                ""
            }
        } else {
            ""
        }
    }

    private fun showCreateEditDialog(
        isInEditingMode: Boolean,
        activity: Activity,
        item: MainCardItem? = null
    ) {

        // Gets the clipboard
        val defaultUrl = urlFromClipboardOrEmpty(item == null)

        val listOfItems = mutableListOf<FormInputText>().apply {
            add(FormInputText(item?.site?.title ?: "", getString(R.string.title), Forms.NAME))
            add(FormInputText(item?.site?.url ?: defaultUrl, getString(R.string.url), Forms.URL))
        }

        val errorOnLastSync = isInEditingMode && item?.site?.isSuccessful == false

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
            ColorPickerRecyclerViewItem(selectedColor, GradientColors.gradients) {
                dialogItemTitle.gradientColors = it
                dialogItemTitle.notifyChanged()
            }

        val materialDialog = MaterialDialog.Builder(activity)
            .customView(R.layout.recyclerview, false)
            .negativeText(R.string.cancel)
            .positiveText("Save")
            .autoDismiss(false) // we need this for wiggle/shake effect, else it would dismiss
            .positiveColor(Color.WHITE)
            .onNegative { dialog, _ -> dialog.dismiss() }
            .onPositive { dialog, _ ->

                // This was adapted from an app which was using NoSql. Not the best syntax, but
                // can be adapted for any scenario, kind of a
                // Eureka (https://github.com/xmartlabs/Eureka) for Android.
                val fromForm = Forms.saveData(listOfItems)
                val newTitle = fromForm[Forms.NAME] as? String ?: ""
                val potentialUrl = fromForm[Forms.URL] as? String ?: ""

                if (isInEditingMode && item != null) {
                    if (isUrlWrong(potentialUrl, listOfItems)) {
                        return@onPositive
                    }

                    item.site.url.let { previousUrl ->

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
                    }
                } else {
                    // Some people will forget to put the http:// on the url, so this is going to help them.
                    // This is going to be absolutely sure the current url is invalid, before adding http:// before it.
                    val url = if (!potentialUrl.isValidUrl()) {
                        "http://$potentialUrl"
                    } else potentialUrl

                    // If even after this it is still invalid, we wiggle
                    if (isUrlWrong(url, listOfItems)) {
                        return@onPositive
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
                    sitesList.add(newItem)
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
                    MaterialDialog.Builder(activity)
                        .title(R.string.turn_on_background_sync_title)
                        .content(R.string.turn_on_background_sync_content)
                        .negativeText(R.string.no)
                        .positiveText(R.string.yes)
                        .positiveColor(Color.WHITE)
                        .btnSelector(
                            R.drawable.dialog_positive_button_indigo,
                            DialogAction.POSITIVE
                        )
                        .onPositive { _, _ ->
                            sharedPrefs.edit { putBoolean("backgroundSync", true) }
                            WorkerHelper.updateWorkerWithConstraints(sharedPrefs)
                        }
                        .show()
                }
            }

        if (errorOnLastSync) {
            // tint the dialog orange when there was an error on last sync
            materialDialog
                .btnSelector(R.drawable.dialog_positive_button_orange, DialogAction.POSITIVE)
                .negativeColorRes(R.color.md_deep_orange_A200)
        } else {
            materialDialog
                .btnSelector(R.drawable.dialog_positive_button_indigo, DialogAction.POSITIVE)
        }

        val materialdialog = materialDialog.build()

        materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
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

        // This will call the keyboard when dialog is shown.
        materialdialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        materialdialog.show()
    }

    private fun isUrlWrong(url: String, listOfItems: MutableList<FormInputText>): Boolean {
        if (!url.isValidUrl()) {
            listOfItems.first { it.kind == Forms.URL }.shakeIt()
            Toasty.error(requireContext(), getString(R.string.incorrect_url)).show()
            return true
        }
        return false
    }
}

