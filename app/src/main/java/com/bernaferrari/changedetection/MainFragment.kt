package com.bernaferrari.changedetection

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.work.State
import androidx.work.WorkStatus
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.source.local.SiteAndLastDiff
import com.bernaferrari.changedetection.forms.FormSection
import com.bernaferrari.changedetection.forms.FormSingleEditText
import com.bernaferrari.changedetection.forms.Forms
import com.bernaferrari.changedetection.groupie.DialogItem
import com.bernaferrari.changedetection.groupie.MainScreenCardItem
import com.bernaferrari.changedetection.ui.ListPaddingDecoration
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.state_layout.view.*
import kotlinx.android.synthetic.main.todos_encontros_activity.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainFragment : Fragment() {
    private lateinit var mViewModel: MainViewModel
    private var sitesList = mutableListOf<MainScreenCardItem>()
    private var sitesSection = Section(sitesList)

    val greyColor: Int by lazy { ContextCompat.getColor(requireActivity(), R.color.FontStrong) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkerHelper.updateWorkerWithConstraints(Application.instance!!.sharedPrefs("workerPreferences"))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.todos_encontros_activity, container, false)
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
                    setEmptyText("No websites are being monitored")
                }, this)
            }
        }

        groupAdapter.setOnItemLongClickListener { item, _ ->
            return@setOnItemLongClickListener if (item is MainScreenCardItem) {
                showDialogWithOptions(item)
                true
            } else {
                false
            }
        }

        groupAdapter.setOnItemClickListener { item, _ ->
            if (item is MainScreenCardItem) {
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

    private fun showDialogWithOptions(item: MainScreenCardItem) {
        val context = requireActivity()

        val customView =
            layoutInflater.inflate(R.layout.default_recycler_grey_200, view!!.parentLayout, false)
        val materialdialog = BottomSheetDialog(requireContext())
        materialdialog.setContentView(customView)
        materialdialog.show()

        val updating = mutableListOf<DialogItem>()

        updating += DialogItem(
            getString(R.string.reload),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_reload).color(greyColor),
            "fetchFromServer"
        )

        updating += DialogItem(
            getString(R.string.edit),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_pencil).color(greyColor),
            "edit"
        )

        // if item is disabled, makes no sense to enable/disable the notifications
        if (item.site.isSyncEnabled) {
            updating += DialogItem(
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

        updating += DialogItem(
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

        updating += DialogItem(
            getString(R.string.remove),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_delete).color(greyColor),
            "remove"
        )

        customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
            adapter = GroupAdapter<ViewHolder>().apply {
                add(Section(updating))

                setOnItemClickListener { dialogitem, _ ->
                    if (dialogitem !is DialogItem) return@setOnItemClickListener

                    when (dialogitem.kind) {
                        "edit" -> showCreateEditDialog(
                            true,
                            context,
                            item as? MainScreenCardItem
                        )
                        "isSyncEnabled" -> {
                            item.site.copy(isSyncEnabled = !item.site.isSyncEnabled).run {
                                mViewModel.updateSite(this)
                                item.enableOrDisable(this)
                                sort()
                            }
                        }
                        "isNotificationEnabled" -> {
                            item.site.copy(isNotificationEnabled = !item.site.isNotificationEnabled)
                                .run {
                                    mViewModel.updateSite(this)
                                    item.enableOrDisable(this)
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
            Toasty.success(requireContext(), "Result has succeded").show()
        }
    }

    private val reloadCallback = { item: MainScreenCardItem ->
        reload(item, true)
    }

    private fun updateList(mutable: MutableList<SiteAndLastDiff>?) {
        view?.stateLayout?.showEmptyState()

        if (mutable == null) {
            return
        }

        if (sitesList.isNotEmpty()) {
            //Verifies if list is not empty and add values that are not there. Basically, makes a diff.
            mutable.forEach { siteAndLastDiff ->
                // if item from new list is curerently on the list, update it. Else, add.
                sitesList.find { carditem -> carditem.site.id == siteAndLastDiff.site.id }.also {
                    if (it == null) {
                        sitesList.add(
                            MainScreenCardItem(
                                siteAndLastDiff.site,
                                siteAndLastDiff.diff,
                                reloadCallback
                            )
                        )
                    } else {
                        it.updateSiteDiff(siteAndLastDiff.site, siteAndLastDiff.diff)
                    }
                }
            }
        } else {
            mutable.mapTo(sitesList) { MainScreenCardItem(it.site, it.diff, reloadCallback) }
        }

        sitesList.sortByDescending { it.lastDiff?.timestamp }
        sitesSection.update(sitesList)

        // This will be used to automatically sync when app open. Since the variable is on ViewModel,
        // even if we navigate between the app, come back and this fragment's onCreate is called again,
        // the variable will not change.
        if (mViewModel.shouldSyncWhenAppOpen) {
            sitesList.forEach(this@MainFragment::reloadEach)
            mViewModel.shouldSyncWhenAppOpen = false
        }
    }

    private fun reloadEach(item: MainScreenCardItem?) {
        reload(item, false)
    }

    private fun reload(item: MainScreenCardItem?, force: Boolean = false) {
        if (item !is MainScreenCardItem || (!item.site.isSyncEnabled && !force)) {
            return
        }

        item.startSyncing()
        launch {
            val strFetched = WorkerHelper.fetchFromServer(item.site)
            if (strFetched != null) {
                launch(UI) { subscribe(strFetched, item) }
            } else {
                // This will happen when internet connection is missing
                launch(UI) {
                    Toasty.error(requireContext(), getString(R.string.missing_internet))
                    item.updateSite(item.site)
                    sitesSection.update(sitesList)
                }
            }
        }
    }

    private fun subscribe(str: String, item: MainScreenCardItem) {
        Logger.d("count size -> ${str.count()}")

        val newSite = item.site.copy(
            timestamp = System.currentTimeMillis(),
            isSuccessful = !(str.count() == 0 || str.isBlank())
        )
        mViewModel.updateSite(newSite)

        val diff = Diff(mViewModel.currentTime(), str.count(), item.site.id, str)
        mViewModel.saveWebsite(diff).observe(this, Observer {
            item.updateSite(newSite)

            if (it == true) {
                Logger.d("Diff: " + diff.diffId)
                item.updateDiff(diff)
                Toasty.success(
                    requireContext(),
                    getString(R.string.was_updated, newSite.title)
                ).show()

                sort()
            }
        })
    }

    private fun sort() {
        sitesList.sortWith(compareBy<MainScreenCardItem> { it.site.isSyncEnabled }.thenBy { it.lastDiff?.timestamp })
        sitesSection.update(sitesList)
    }

    private fun removeMy(item: MainScreenCardItem?) {
        if (item != null) {
            sitesList.remove(item)
            sitesSection.update(sitesList)
            mViewModel.removeSite(item.site)
        }
    }

    private fun showCreateEditDialog(
        isInEditingMode: Boolean,
        activity: Activity,
        item: MainScreenCardItem? = null
    ) {

        val listOfItems = mutableListOf<Item>().apply {
            add(FormSection(getString(R.string.url), true))
            add(FormSingleEditText(item?.site?.url ?: "", Forms.URL))

            add(FormSection(getString(R.string.title), true))
            add(FormSingleEditText(item?.site?.title ?: "", Forms.NAME))
        }

        val materialdialogpiece = MaterialDialog.Builder(activity)
            .customView(R.layout.default_recycler_grey_200, false)
            .negativeText(R.string.cancel)
            .onPositive { _, _ ->
                val fromForm = Forms.saveData(listOfItems)
                Logger.d(fromForm)

                val newTitle = fromForm[Forms.NAME] as? String ?: ""
                val newUrl = fromForm[Forms.URL] as? String ?: ""

                if (isInEditingMode && item != null) {
                    val updatedSite = item.site.copy(
                        title = newTitle,
                        url = newUrl
                    )
                    val previousUrl = item.site.url
                    // Update internally
                    mViewModel.updateSite(updatedSite)
                    item.updateSite(updatedSite)
                    // Only fetchFromServer if the url has changed.
                    if (newUrl != previousUrl) {
                        reload(item, true)
                    }
                } else {
                    // Some people will forget to put the http:// on the url, so this is going to help them.
                    val url = when {
                        !newUrl.startsWith("http://") && !newUrl.startsWith("https://") -> "http://$newUrl"
                        else -> newUrl
                    }

                    if (url.isBlank()) {
                        return@onPositive
                    }

                    val site = mViewModel.saveSite(newTitle, url, mViewModel.currentTime())
                    // add and sort the card
                    val newItem = MainScreenCardItem(site, null, reloadCallback)
                    sitesList.add(newItem)
                    sitesSection.update(sitesList)
                    reload(newItem, true)
                    // It is putting the new item on the last position before refreshing.
                    // This is not good UX since user won't know it is there, specially when
                    // the url results in error.
                }
            }

        when (isInEditingMode) {
            true ->
                materialdialogpiece
                    .title("Edit")
                    .positiveText("Save")
            false ->
                materialdialogpiece
                    .title("Add")
                    .positiveText("Save")
        }

        val materialdialog = materialdialogpiece.build()

        materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
            layoutManager = LinearLayoutManager(this.context)
            adapter = GroupAdapter<ViewHolder>().apply {
                add(Section(listOfItems))
            }
        }

        materialdialog.show()
    }

    fun obtainViewModel(activity: FragmentActivity): MainViewModel {
        // Use a Factory to inject dependencies into the ViewModel
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProviders.of(activity, factory).get(MainViewModel::class.java)
    }
}

