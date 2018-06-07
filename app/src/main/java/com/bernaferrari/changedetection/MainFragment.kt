package com.bernaferrari.changedetection

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
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
import androidx.navigation.Navigation
import androidx.work.State
import androidx.work.WorkStatus
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.local.SiteAndLastSnap
import com.bernaferrari.changedetection.extensions.isValidUrl
import com.bernaferrari.changedetection.forms.FormInputText
import com.bernaferrari.changedetection.forms.Forms
import com.bernaferrari.changedetection.groupie.ColorPickerRecyclerViewItem
import com.bernaferrari.changedetection.groupie.DialogItemSimple
import com.bernaferrari.changedetection.groupie.DialogItemTitle
import com.bernaferrari.changedetection.groupie.MainCardItem
import com.bernaferrari.changedetection.ui.ListPaddingDecoration
import com.bernaferrari.changedetection.util.GradientColors
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment.view.*
import kotlinx.android.synthetic.main.state_layout.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainFragment : Fragment() {
    private lateinit var mViewModel: MainViewModel
    private var sitesList = mutableListOf<MainCardItem>()
    private var sitesSection = Section(sitesList)

    private val greyColor: Int by lazy {
        ContextCompat.getColor(
            requireActivity(),
            R.color.FontStrong
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkerHelper.updateWorkerWithConstraints(Application.instance!!.sharedPrefs("workerPreferences"))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.main_fragment, container, false)
        mViewModel = obtainViewModel(requireActivity())
        val groupAdapter = GroupAdapter<ViewHolder>()

        // Clear it up, just in case of rotation.
        sitesSection.update(sitesList.apply { clear() })

        view.run {
            stateLayout.showLoading()

            settings.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .add(SettingsFragment(), "settings").commit()
            }

            info.setOnClickListener {
                Navigation.findNavController(view)
                    .navigate(R.id.action_mainFragment_to_aboutFragment)
            }

            fab.run {
                background = IconicsDrawable(requireActivity(), CommunityMaterial.Icon.cmd_plus)
                setOnClickListener { showCreateEditDialog(false, requireActivity()) }
            }

            pullToRefresh.let { mSwipeRefreshLayout ->
                mSwipeRefreshLayout.setOnRefreshListener {
                    sitesList.forEach(this@MainFragment::reloadEach)
                    mSwipeRefreshLayout.isRefreshing = false
                }
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
            if (item is MainCardItem) {
                val bundle = bundleOf(
                    "SITEID" to item.site.id,
                    "TITLE" to item.site.title,
                    "URL" to item.site.url
                )
                Navigation.findNavController(view)
                    .navigate(R.id.action_mainFragment_to_openFragment, bundle)
            }
        }

        mViewModel.loadSites().observe(this, Observer(::updateList))
        mViewModel.getOutputStatus().observe(this, Observer(::workOutput))

        return view
    }

    private fun showDialogWithOptions(item: MainCardItem) {
        val context = requireActivity()

        val customView =
            layoutInflater.inflate(R.layout.recyclerview, view!!.parentLayout, false)
        val materialdialog = BottomSheetDialog(requireContext())
        materialdialog.setContentView(customView)
        materialdialog.show()

        val chart = Section()
        val updating = mutableListOf<DialogItemSimple>()

        updating += DialogItemSimple(
            getString(R.string.reload),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_reload).color(greyColor),
            "fetchFromServer"
        )

        updating += DialogItemSimple(
            getString(R.string.edit),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_pencil).color(greyColor),
            "edit"
        )

        updating += DialogItemSimple(
            getString(R.string.open_in_browser),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_google_chrome).color(greyColor),
            "openInBrowser"
        )

//        Code is working but this isn't the right moment to put it.
//        launch {
//            val minimalDiffs = mViewModel.getRecentMinimalSnaps(item.site.id)
//            if (minimalDiffs != null && minimalDiffs.size > 2){
//                val chartItem = ItemChart(minimalDiffs, item.site.isSuccessful)
//                launch (UI) { chart.update(mutableListOf(chartItem)) }
//            }
//        }

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
                    .color(greyColor),
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
                .color(greyColor),
            "isSyncEnabled"
        )

        updating += DialogItemSimple(
            getString(R.string.remove),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_delete).color(greyColor),
            "remove"
        )

        customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
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
                            item.site.copy(isSyncEnabled = !item.site.isSyncEnabled).run {
                                mViewModel.updateSite(this)
                                item.update(this)
                                sort()
                            }
                        }
                        "isNotificationEnabled" -> {
                            item.site.copy(isNotificationEnabled = !item.site.isNotificationEnabled)
                                .run {
                                    mViewModel.updateSite(this)
                                    item.update(this)
                                    sort()
                                }
                        }
                        "fetchFromServer" -> reload(item, true)
                        "remove" -> removeMy(item)
                    }
                    materialdialog.dismiss()
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

        if (sitesList.isNotEmpty()) {
            //Verifies if list is not empty and add values that are not there. Basically, makes a minimalSnap.
            mutable.forEach { siteAndLastSnap ->
                // if item from new list is curerently on the list, update it. Else, add.
                sitesList.find { carditem -> carditem.site.id == siteAndLastSnap.site.id }.also {
                    if (it == null) {
                        sitesList.add(
                            MainCardItem(
                                siteAndLastSnap.site,
                                siteAndLastSnap.minimalSnap,
                                reloadCallback
                            )
                        )
                    } else {
                        it.update(siteAndLastSnap.site, siteAndLastSnap.minimalSnap)
                    }
                }
            }
        } else {
            mutable.mapTo(sitesList) { MainCardItem(it.site, it.minimalSnap, reloadCallback) }
        }

        sitesList.sortByDescending { it.lastMinimalSnap?.timestamp }
        sitesSection.update(sitesList)
        sort()

        // This will be used to automatically sync when app open. Since the variable is on ViewModel,
        // even if we navigate between the app, come back and this fragment's onCreate is called again,
        // the variable will not change.
        if (mViewModel.shouldSyncWhenAppOpen) {
            sitesList.forEach(this@MainFragment::reloadEach)
            mViewModel.shouldSyncWhenAppOpen = false
        }
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
            if (strFetched != null) {
                launch(UI) { updateSiteAndSnap(strFetched.first, strFetched.second, item) }
            } else {
                // This will happen when internet connection is missing
                launch(UI) {
                    Toasty.error(requireContext(), getString(R.string.missing_internet))
                    item.update(item.site)
                    sitesSection.update(sitesList)
                }
            }
        }
    }

    private fun updateSiteAndSnap(contentType: String, value: ByteArray, item: MainCardItem) {
        Logger.d("count size -> ${value.size}")

        val newSite = item.site.copy(
            timestamp = System.currentTimeMillis(),
            isSuccessful = value.isNotEmpty()
        )
        mViewModel.updateSite(newSite)

        val snap = Snap(item.site.id, newSite.timestamp, contentType, value.size, value)
        mViewModel.saveWebsite(snap).observe(this, Observer {
            item.update(newSite)

            if (it != true) {
                return@Observer
            }

            Logger.d("Snap: " + snap.snapId)

            if (item.lastMinimalSnap != null) {
                // Only show this toast when there was a change, which means, not on the first sync.
                Toasty.success(
                    requireContext(),
                    getString(
                        R.string.was_updated,
                        newSite.title?.takeIf { it.isNotBlank() } ?: newSite.url)
                ).show()
            }

            item.update(snap)
            sort()
        })
    }

    private fun sort() {
        // sort by active/inactive, then by timestamp of the last snapshot, then by item title, and if they are still the same, by the url
        sitesList.sortWith(compareByDescending<MainCardItem> { it.site.isSyncEnabled }.thenByDescending { it.lastMinimalSnap?.timestamp }.thenBy { it.site.title }.thenBy { it.site.url })
        sitesSection.update(sitesList)
    }

    private fun removeMy(item: MainCardItem?) {
        if (item != null) {
            sitesList.remove(item)
            sitesSection.update(sitesList)
            mViewModel.removeSite(item.site)
        }
    }

    private fun showCreateEditDialog(
        isInEditingMode: Boolean,
        activity: Activity,
        item: MainCardItem? = null
    ) {

        val listOfItems = mutableListOf<FormInputText>().apply {
            add(FormInputText(item?.site?.url ?: "", getString(R.string.url), Forms.URL))
            add(FormInputText(item?.site?.title ?: "", getString(R.string.title), Forms.NAME))
        }

        val errorOnLastSync = isInEditingMode && item?.site?.isSuccessful == false

        val colorsList = GradientColors.getGradients()

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

        val dialogItemColorPicker = ColorPickerRecyclerViewItem(selectedColor, colorsList) {
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

                    val site = mViewModel.saveSite(
                        newTitle,
                        url,
                        mViewModel.currentTime(),
                        dialogItemTitle.gradientColors
                    )
                    // add and sort the card
                    val newItem = MainCardItem(site, null, reloadCallback)
                    sitesList.add(newItem)
                    sitesSection.update(sitesList)
                    // Scroll down, so user can see there is a new item.
                    defaultRecycler.smoothScrollToPosition(sitesList.size - 1)
                    reload(newItem, true)
                }
                dialog.dismiss()

                val sharedPrefs = Application.instance!!.sharedPrefs("workerPreferences")
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
        materialdialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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

    private fun obtainViewModel(activity: FragmentActivity): MainViewModel {
        // Use a Factory to inject dependencies into the ViewModel
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProviders.of(activity, factory).get(MainViewModel::class.java)
    }
}

