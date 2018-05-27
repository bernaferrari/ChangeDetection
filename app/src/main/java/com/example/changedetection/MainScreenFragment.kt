package com.example.changedetection

import android.app.Activity
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.work.State
import com.afollestad.materialdialogs.MaterialDialog
import com.example.changedetection.data.Diff
import com.example.changedetection.data.source.local.SiteAndLastDiff
import com.example.changedetection.forms.*
import com.example.changedetection.groupie.BottomSheetCardItem
import com.example.changedetection.groupie.DialogItem
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
    private lateinit var mViewModel: TasksViewModel
    private val groupAdapter = GroupAdapter<ViewHolder>()
    private var sitesList = mutableListOf<BottomSheetCardItem>()
    private var sitesSection = Section(sitesList)

    val color: Int by lazy { ContextCompat.getColor(requireActivity(), R.color.FontStrong) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkerHelper.updateWorkerWithConstraints(Application.instance!!.sharedPrefs("workerPreferences"))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.todos_encontros_activity, container, false)

        mViewModel = MainActivity.obtainViewModel(requireActivity())

        view.fab.run {
            background = IconicsDrawable(requireActivity(), CommunityMaterial.Icon.cmd_plus)
            setOnClickListener {
                criarEditarDialog(false, requireActivity())
            }
        }

        view.settings.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().add(FragSettings(), "settings").commit()
        }

        view.stateLayout.showLoading()

        view.defaultRecycler.run {
            if (view.stateLayout == null){
                println("null!")
            }

            addItemDecoration(ListPaddingDecoration(this.context))

            itemAnimator = null
            adapter = groupAdapter.apply {
                if (this.itemCount == 0) {
//                    this.add(MarqueeItem("Change Detection"))
                    this.add(sitesSection)
                }
            }

            setEmptyView(view.stateLayout.apply {
                setEmptyText("No websites are being monitored")
            }, this)

            layoutManager = LinearLayoutManager(context)
        }

        view.pullToRefresh.let { mSwipeRefreshLayout ->
            mSwipeRefreshLayout.setOnRefreshListener {
                sitesList.forEach(this::reload)
                mSwipeRefreshLayout.isRefreshing = false
            }
        }

        groupAdapter.setOnItemLongClickListener { item, view ->
            if (item is BottomSheetCardItem) {

                val context = requireActivity()

                val materialdialog = MaterialDialog.Builder(context)
                    .customView(R.layout.default_recycler_grey_200, false)
                    .show()

                val updating = mutableListOf<DialogItem>()

                updating += DialogItem(
                    "Reload",
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_reload).color(
                        ContextCompat.getColor(context, R.color.md_green_500)
                    ),
                    "fetchFromServer"
                )

                updating += DialogItem(
                    "Edit",
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_pencil).color(
                        ContextCompat.getColor(context, R.color.md_blue_500)
                    ),
                    "edit"
                )

                updating += DialogItem(
                    "Remove",
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_close).color(
                        ContextCompat.getColor(context, R.color.md_red_500)
                    ),
                    "remove"
                )

                materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
                    adapter = GroupAdapter<ViewHolder>().apply {
                        add(Section(updating))

                        setOnItemClickListener { dialogitem, view ->
                            if (dialogitem !is DialogItem) return@setOnItemClickListener

                            when (dialogitem.kind) {
                                "edit" -> criarEditarDialog(
                                    true,
                                    context,
                                    item as? BottomSheetCardItem
                                )
                                "fetchFromServer" -> reload(item)
                                "remove" -> removeMy(item)
                            }
                            materialdialog.dismiss()
                        }
                    }
                }
                return@setOnItemLongClickListener true
            }
            return@setOnItemLongClickListener false
        }

        groupAdapter.setOnItemClickListener { item, _ ->
            when (item) {
                is BottomSheetCardItem -> {
                    val bundle = bundleOf(
                        "TASKID" to item.tas.id,
                        "TITLE" to item.tas.title,
                        "URL" to item.tas.url
                    )
                    Navigation.findNavController(view).navigate(R.id.action_mainFragment_to_openFragment, bundle)
                }
            }
        }

        mViewModel.getOutputStatus()
            .observe(this, Observer {
                val result = it ?: return@Observer
                val sb = StringBuilder()
                result.forEach { sb.append("${it.id}: ${it.state.name}\n") }
                sb.setLength(sb.length - 1) // Remove the last \n from the string
                Toasty.info(requireContext(), sb).show()

                if (result.firstOrNull()?.state == State.SUCCEEDED){
                    Toasty.success(requireContext(), "Result has succeded").show()
                }
        })

        mViewModel.loadTasks()

        mViewModel.showEmptyOnMain.observe(this, Observer {
            view.stateLayout.showEmptyState()
        })

        mViewModel.items.observe(this, Observer {
            if (it != null){
                updateList(it)
            }
        })

        return view
    }

    private fun updateList(mutable: MutableList<SiteAndLastDiff>){
        if (sitesList.isNotEmpty()){
            //Verifies if list is not empty and add values that are not there. Basically, makes a diff.
            mutable.forEach { siteAndLastDiff ->
                // if item from new list is curerently on the list, update it. Else, add.
                sitesList.find { carditem -> carditem.tas.id == siteAndLastDiff.site.id }.also {
                    if (it == null){
                        sitesList.add(BottomSheetCardItem(siteAndLastDiff.site, siteAndLastDiff.diff, globalListener))
                    } else {
                        it.updateTaskDiff(siteAndLastDiff.site, siteAndLastDiff.diff)
                    }
                }
            }
        } else {
            mutable.mapTo(sitesList) { BottomSheetCardItem(it.site, it.diff, globalListener) }
        }

        sitesList.sortByDescending { it.lastDiff?.timestamp }
        sitesSection.update(sitesList)
    }

    interface Listeners {
        fun onClickListener(item: BottomSheetCardItem)
        fun onLongClickListener()
        fun onRefreshListener(item: BottomSheetCardItem)
    }

    private val globalListener = object : Listeners {
        override fun onClickListener(item: BottomSheetCardItem) {
            if (view != null) {
                val bundle = bundleOf(
                        "TASKID" to item.tas.id,
                        "TITLE" to item.tas.title,
                        "URL" to item.tas.url
                )
                Navigation.findNavController(view!!).navigate(R.id.action_mainFragment_to_openFragment, bundle)
            }
        }
        override fun onLongClickListener() = Unit
        override fun onRefreshListener(item: BottomSheetCardItem) = reload(item)
    }

    private fun reload(item: BottomSheetCardItem?) {
        if (item !is BottomSheetCardItem) {
            return
        }

        item.startSyncing()
        launch {
            val strFetched = WorkerHelper.fetchFromServer(item.tas)
            launch (UI) { subscribe(strFetched!!, item) }
        }
    }

    private fun subscribe(str: String, item: BottomSheetCardItem){
        Logger.d("count size -> ${str.count()}")

        val newTask = item.tas.copy(timestamp = System.currentTimeMillis(), successful = !(str.count() == 0 || str.isBlank()))
        mViewModel.updateTask(newTask)

        val diff = Diff(str, mViewModel.currentTime(), item.tas.id)
        mViewModel.saveWebsite(diff).observe(this, Observer {
            item.updateTask(newTask)

            if (it == true) {
                Logger.d("Diff: " + diff.fileId)
                item.updateDiff(diff)
                Toasty.success(requireContext(), "${newTask.title} was updated!", Snackbar.LENGTH_SHORT).show()
                sitesList.sortByDescending { it.lastDiff?.timestamp }
                sitesSection.update(sitesList)
            }
        })
    }

    private fun removeMy(item: BottomSheetCardItem?) {
        if (item != null) {
            sitesList.remove(item)
            sitesSection.update(sitesList)
            mViewModel.removeTask(item.tas)
        }
    }

    private fun criarEditarDialog(
        isInEditingMode: Boolean,
        activity: Activity,
        item: BottomSheetCardItem? = null
    ) {

        val listOfItems = mutableListOf<Item>().apply {
            add(FormSection("Url", true))
            add(FormSingleEditText(item?.tas?.url ?: "", Forms.URL))

            add(FormSection("Title", true))
            add(FormSingleEditText(item?.tas?.title ?: "", Forms.NAME))
        }

        val materialdialogpiece = MaterialDialog.Builder(activity)
            .customView(R.layout.default_recycler_grey_200, false)
            .negativeText(R.string.cancel)
            .onPositive { _, _ ->
                val map2 = Forms.saveData(listOfItems, mutableListOf())
                Logger.d(map2)

                val newTitle = map2["name"] as? String ?: ""
                val newUrl = map2["url"] as? String ?: ""

                if (isInEditingMode && item != null) {
                    val updatedTask = item.tas.copy(
                        title = newTitle,
                        url = newUrl
                    )
                    val previousUrl = item.tas.url
                    // Update internally
                    mViewModel.updateTask(updatedTask)
                    item.updateTask(updatedTask)
                    // Only fetchFromServer if the url has changed.
                    if (newUrl != previousUrl){
                        reload(item)
                    }
                } else {
                    val url = when {
                        !newUrl.startsWith("http://") && !newUrl.startsWith("https://") -> "http://$newUrl"
                        else -> newUrl
                    }

                    if (url.isBlank()){
                        return@onPositive
                    }

                    val task = mViewModel.saveTask(newTitle, url, mViewModel.currentTime())
                    // add and sort the card
                    val newItem = BottomSheetCardItem(task, null, globalListener)
                    sitesList.add(newItem)
                    sitesSection.update(sitesList)
                    reload(newItem)
                    // It is putting the new item on the last position before refreshing.
                    // This is not good UX since user won't know it is there,
                    // specially when the url results in error.
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
}

