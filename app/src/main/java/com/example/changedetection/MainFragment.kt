package com.example.changedetection

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.afollestad.materialdialogs.MaterialDialog
import com.example.changedetection.data.Diff
import com.example.changedetection.data.Site
import com.example.changedetection.forms.*
import com.example.changedetection.groupie.BottomSheetCardItem
import com.example.changedetection.groupie.DialogItem
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.empty_layout.view.*
import kotlinx.android.synthetic.main.state_layout.view.*
import kotlinx.android.synthetic.main.todos_encontros_activity.view.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MainFragment : Fragment() {
    private lateinit var mViewModel: TasksViewModel
    private val groupAdapter = GroupAdapter<ViewHolder>()
    private var theList = mutableListOf<BottomSheetCardItem>()
    private var updatingCultos = Section(theList)

    val color: Int by lazy { ContextCompat.getColor(requireActivity(), R.color.FontStrong) }

    fun dp(value: Int, resources: Resources): Int {
        return (resources.displayMetrics.density * value).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        WorkManager.getInstance().cancelUniqueWork("sample")
        
        val photoWork = PeriodicWorkRequest.Builder(
            SyncWorker::class.java,
            15,
            TimeUnit.SECONDS)
            .addTag("sample")
            .build()

//        WorkManager.getInstance().enqueue(photoWork)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.todos_encontros_activity, container, false)
//    }
//
//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)

        mViewModel = MainActivity.obtainViewModel(requireActivity())

        view.fab.run {
            background = IconicsDrawable(requireActivity(), CommunityMaterial.Icon.cmd_plus)
            setOnClickListener {
                criarEditarDialog(false, requireActivity())
            }
        }

        view.defaultRecycler.apply {
            if (view.stateLayout == null){
                println("null!")
            }

            setEmptyView(view.stateLayout.apply {
                this.empty_text.text = "Nothing found"
                showEmptyState()
            })

            itemAnimator = null
            adapter = groupAdapter.apply {
                spanCount = calculateNoOfColumns(requireActivity())
                if (this.itemCount == 0) {
                    this.add(MarqueeItem("Change Detection"))
                    this.add(updatingCultos)
                }
            }

            layoutManager = GridLayoutManager(context, groupAdapter.spanCount).apply {
                spanSizeLookup = groupAdapter.spanSizeLookup
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
                    "reload"
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
                                "reload" -> reload(item)
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

        groupAdapter.setOnItemClickListener { item, view ->

            Logger.d("Getting inside site id: " + (item as? BottomSheetCardItem)?.tas?.id)

            if (item is BottomSheetCardItem) {

                val bundle =
                    bundleOf("TASKID" to item.tas.id, "TITLE" to item.tas.title, "URL" to item.tas.url)
                Navigation.findNavController(view).navigate(R.id.action_mainFragment_to_openFragment, bundle)
            }
        }

        WorkManager.getInstance()
            .getStatusesByTag("sample")
            .observe(this, Observer {
            val result = it ?: return@Observer

            val sb = StringBuilder()
            result.forEach { sb.append("${it.id}: ${it.state.name}\n") }
            Toast.makeText(requireContext(), sb.toString(), Toast.LENGTH_LONG).show()

                result.forEach {
                    WorkManager.getInstance().cancelAllWorkByTag("sample")
                }

            theList.forEach { it.startSyncing() }
            mViewModel.loadTasks(true)
        })

        mViewModel.loadTasks(false)

        mViewModel.items2.observe(this, Observer { mutable ->

            if (theList.isNotEmpty()){
                //Verifies if list is not empty and add values that are not there. Basically, makes a diff.
                mutable?.forEach { siteAndLastDiff ->
                    theList.find { carditem -> carditem.tas.id == siteAndLastDiff.site.id }.also {
                        if (it == null){
                            theList.add(BottomSheetCardItem(siteAndLastDiff.site, siteAndLastDiff.diff, globalListener))
                        } else {
                            it.updateTaskDiff(siteAndLastDiff.site, siteAndLastDiff.diff)
                        }
                    }
                }
            } else {
                mutable?.forEach {
                    theList.add(BottomSheetCardItem(it.site, it.diff, globalListener))
                }
            }
            updatingCultos.update(theList)
        })

        return view
    }

    interface bottom {
        fun onClickListener(id: BottomSheetCardItem)
        fun onLongClickListener()
        fun onRefreshListener(id: BottomSheetCardItem)
    }

    private val globalListener = object : bottom {
        override fun onClickListener(item: BottomSheetCardItem) {
            if (view != null) {
                val bundle =
                    bundleOf("TASKID" to item.tas.id, "TITLE" to item.tas.title, "URL" to item.tas.url)

                Navigation.findNavController(view!!).navigate(R.id.action_mainFragment_to_openFragment, bundle)
            }
        }

        override fun onLongClickListener() {

        }

        override fun onRefreshListener(item: BottomSheetCardItem) {
            reload(item)
        }
    }

//    private var spruceAnimator: Animator? = null
//
//    private fun initSpruce(recyclerView: RecyclerView) {
////        spruceAnimator = Spruce.SpruceBuilder(recyclerView)
////            .sortWith(DefaultSort(150))
////            .animateWith(
////                DefaultAnimations.fadeInAnimator(recyclerView, 300),
////                ObjectAnimator.ofFloat(recyclerView, "translationY", recyclerView.height.toFloat()/2, 0f).setDuration(350))
////            .start()
//    }

    private fun reload(item: BottomSheetCardItem?) {
        if (item !is BottomSheetCardItem) {
            return
        }

        item.startSyncing()
        val client = OkHttpClient()

        Single.fromCallable {
            val request = Request.Builder()
                .url(item.tas.url)
                .build()

            val response1 = client.newCall(request).execute()

            Logger.d("isSuccessful -> ${response1.isSuccessful}")
            Logger.d("header -> ${response1.headers()}")

            val str = response1.body()?.string() ?: ""
//            Logger.d("str -> $str")
            return@fromCallable str
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                Logger.d("onErrorReturn")
                return@onErrorReturn ""
            }
            .subscribe { it ->
                Logger.d("count size -> ${it.count()}")

                val newTask = item.tas.copy(timestamp = System.currentTimeMillis(), successful = !(it.count() == 0 || it.isNullOrBlank()))
                mViewModel.updateTask(newTask)
                item.updateTask(newTask)

                val diff = Diff(it, currentTime(), item.tas.id)
                mViewModel.saveWebsite(diff).observe(this, Observer {
                    if (it == true) {
                        Logger.d("Diff: " + diff.fileId)
                        item.updateDiff(diff)
                    }
                })
            }
    }

    private fun removeMy(item: BottomSheetCardItem?) {
        if (item != null) {
            theList.remove(item)
            updatingCultos.update(theList)
            mViewModel.removeTask(item.tas)
        }
    }

    private fun currentTime(): Long = System.currentTimeMillis()

    private fun criarEditarDialog(
        isInEditingMode: Boolean,
        activity: Activity,
        item: BottomSheetCardItem? = null
    ) {

        val fastFA2 = FastItemAdapter<EmptyAdapter>()

        val materialdialogpiece = MaterialDialog.Builder(activity)
            .customView(R.layout.default_recycler_grey_200, false)
            .negativeText(R.string.cancel)
            .onPositive { _, _ ->
                val map2 = FormSaveAndHook.saveData(fastFA2, mutableListOf())
                Logger.d(map2)

                val urltmp = map2["url"] as? String ?: ""
                val title = map2["name"] as? String ?: ""

                if (isInEditingMode && item != null) {
                    val updatedTask = Site(title, urltmp, currentTime(), item.tas.id)
                    mViewModel.updateTask(updatedTask)
                    theList.firstOrNull { it.tas.id == item.tas.id }?.updateTask(item.tas)
                    reload(item)
//                    theList.remove(item)
//                    theList.add(BottomSheetCardItem(updatedTask, item.lastDiff, globalListener))
//                    updatingCultos.update(theList)
                } else {
                    val url = when {
                        !urltmp.startsWith("http://") && !urltmp.startsWith("https://") -> "http://$urltmp"
                        else -> urltmp
                    }

                    val task = mViewModel.saveTask(title, url, currentTime())
                    // add and sort the card

                    val newItem = BottomSheetCardItem(task, null, globalListener)
                    theList.add(newItem)
                    updatingCultos.update(theList)
                    reload(newItem)
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

        fastFA2.apply {
            add(FormDecSection("Url", FormConstants.url, true))
            add(FormDecSingleText(item?.tas?.url ?: "", FormConstants.url))

            add(FormDecSection("Title (optional)", FormConstants.iname, true))
            add(FormDecSingleText(item?.tas?.title ?: "", FormConstants.iname))
        }

        materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
            layoutManager = LinearLayoutManager(this.context)
            adapter = fastFA2
        }

        materialdialog.show()
    }

    companion object {
        // Useful for landscape mode, tablets and extra extra large displays
        fun calculateNoOfColumns(context: Context): Int {

            val point = Point()
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(
                point
            )

            val metrics = context.resources.displayMetrics
            val ratio = metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
            val displayMetrics = context.resources.displayMetrics
            val dpWidth = displayMetrics.widthPixels / displayMetrics.density

            Logger.d("WIDHT $dpWidth")
            Logger.d("ratio $ratio")
            if (ratio < 1) {
                return (dpWidth / 240).toInt()
            }
            return (dpWidth / 240).toInt()
        }
    }
}

