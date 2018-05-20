package com.example.changedetection

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Point
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import com.orhanobut.logger.Logger

class MainActivity : AppCompatActivity() {
//    private lateinit var mViewModel: TasksViewModel
//    private val groupAdapter = GroupAdapter<ViewHolder>()
//    private var theList = mutableListOf<Item>()
//    private var updatingCultos = Section(theList)
//
//    val color: Int by lazy { ContextCompat.getColor(this, R.color.FontStrong) }
//
//    fun dp(value: Int, resources: Resources): Int {
//        return (resources.displayMetrics.density * value).toInt()
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.todos_encontros_activity)
//
//        mViewModel = obtainViewModel(this)
//
//        val compressionWork = OneTimeWorkRequest.Builder(SyncWorker::class.java).build()
//        WorkManager.getInstance().enqueue(compressionWork)
//
//        val div = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
//        div.setDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.divider2)!!)
//
//        fab.background = IconicsDrawable(this, CommunityMaterial.Icon.cmd_plus)
//        fab.setOnClickListener {
//            criarEditarDialog(false, this)
//        }
//
//        stateLayout.empty_text.text = "Nothing found"
//        stateLayout.showEmptyState()
//
//        groupAdapter.apply {
//            spanCount = calculateNoOfColumns(this@MainActivity)
//        }
//
//        defaultRecycler.apply {
//            this.setEmptyView(stateLayout)
//            addItemDecoration(div)
//            adapter = groupAdapter
//
//            val linearLayoutManager = object : GridLayoutManager(context, groupAdapter.spanCount) {
//                override fun onLayoutChildren(
//                    recycler: RecyclerView.Recycler?,
//                    state: RecyclerView.State
//                ) {
//                    super.onLayoutChildren(recycler, state)
//                    initSpruce(this@apply)
//                }
//            }
//
//            layoutManager = linearLayoutManager.apply {
//                spanSizeLookup = groupAdapter.spanSizeLookup
//            }
//        }
//
//        groupAdapter.add(MarqueeItem("Change Detection"))
//        groupAdapter.add(updatingCultos)
//        groupAdapter.setOnItemLongClickListener { item, view ->
//            if (item is BottomSheetCardItem) {
//
//                val context = this
//
//                val materialdialog = MaterialDialog.Builder(this)
//                    .customView(R.layout.default_recycler_grey_200, false)
//                   .show()
//
//                val updating = mutableListOf<DialogItem>()
//
//                updating += DialogItem(
//                    "Reload",
//                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_reload).color(
//                        ContextCompat.getColor(context, R.color.md_green_500)
//                    ),
//                    "reload"
//                )
//
//                updating += DialogItem(
//                    "Edit",
//                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_pencil).color(
//                        ContextCompat.getColor(context, R.color.md_blue_500)
//                    ),
//                    "edit"
//                )
//
//                updating += DialogItem(
//                    "Remove",
//                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_close).color(
//                        ContextCompat.getColor(context, R.color.md_red_500)
//                    ),
//                    "remove"
//                )
//
//                materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
//                    adapter = GroupAdapter<ViewHolder>().apply {
//                        add(Section(updating))
//
//                        setOnItemClickListener { dialogitem, view ->
//                            if (dialogitem !is DialogItem) return@setOnItemClickListener
//
//                            when (dialogitem.kind){
//                                "edit" -> criarEditarDialog(true, context, item as? BottomSheetCardItem)
//                                "reload" -> reload(item)
//                                "remove" -> removeMy(item)
//                            }
//                            materialdialog.dismiss()
//                        }
//                    }
//                }
////                criarEditarDialog(true, this, item as? BottomSheetCardItem)
//                return@setOnItemLongClickListener true
//            }
//            return@setOnItemLongClickListener false
//        }
//
//        groupAdapter.setOnItemClickListener { item, view ->
//
//            Logger.d("Getting inside site id: " + (item as? BottomSheetCardItem)?.tas?.id)
//
//            if (item is BottomSheetCardItem) {
//
//                supportFragmentManager.run {
//                    beginTransaction()
//                        .setCustomAnimations(
//                            R.anim.slide_up2,
//                            R.anim.design_bottom_sheet_slide_out,
//                            R.anim.slide_up2,
//                            R.anim.design_bottom_sheet_slide_out
//                        )
//                        .add(
//                            R.id.framelayout,
//                            OpenFragment.newInstance(item.tas.id, item.title, item.tas.url),
//                            "elasticsheet"
//                        )
//                        .addToBackStack(null)
//                        .commitAllowingStateLoss()
//                }
//            }
//        }
//
//        mViewModel.loadTasks(false)
//
//        mViewModel.items2.observe(this, Observer {
//            theList.clear()
//            it?.forEach {
//                theList.add(BottomSheetCardItem(it.site, it.diff))
//            }
//            updatingCultos.update(theList)
//        })
//    }
//
//    private var spruceAnimator: Animator? = null
//
//    private fun initSpruce(recyclerView: RecyclerView) {
//        spruceAnimator = Spruce.SpruceBuilder(recyclerView)
//            .sortWith(DefaultSort(100))
//            .animateWith()
//            .start()
//    }
//
//    private fun reload(item: BottomSheetCardItem?) {
//        if (item !is BottomSheetCardItem) {
//            return
//        }
//
//        item.status = 1
//        item.notifyChanged()
//
//        val client = OkHttpClient()
//
//        Single.fromCallable {
//            val request = Request.Builder()
//                .url(item.tas.url)
//                .build()
//
//            val response1 = client.newCall(request).execute()
//
//            val resp2 = response1.isSuccessful
//
//            Logger.d("isSuccessful -> $resp2")
//            Logger.d(
//                "header -> ${response1.headers()
//                }"
//            )
//
//            val str = response1.body()?.string()
//            Logger.d("str -> $str")
//
//            return@fromCallable str
//        }
//            .subscribeOn(Schedulers.newThread())
//            .observeOn(AndroidSchedulers.mainThread())
//            .onErrorReturn {
//                Logger.d("onErrorReturn")
//                return@onErrorReturn ""
//            }
//            .subscribe { it ->
//                Logger.d("count -> $it")
//
////                item.size = it?.count() ?: 0
////                item.timestamp = currentTime()
//
//                if (it?.count() == 0 || it == null) {
//                    item.status = 2
//                    item.notifyChanged()
//                } else {
//                    item.status = 0
//                    if (it.isNotBlank()) {
//                        val diff = Diff(it, currentTime(), item.tas.id)
//
//                        mViewModel.saveWebsite(diff).observe(this, Observer {
//                            if (it == true) {
//                                item.notifyChanged(diff)
//                            }
//                        })
//                    }
//                }
//
//                item.notifyChanged()
//            }
//    }
//
//    private fun removeMy(item: BottomSheetCardItem?) {
//        if (item != null) {
//            mViewModel.completeTask(item.tas, true)
//            mViewModel.clearCompletedTasks()
//            theList.remove(item)
//            updatingCultos.update(theList)
//        }
//    }
//
//    private fun currentTime(): Long = System.currentTimeMillis()
//
//    private fun criarEditarDialog(
//        isInEditingMode: Boolean,
//        activity: Activity,
//        item: BottomSheetCardItem? = null
//    ) {
//
//        val fastFA2 = FastItemAdapter<EmptyAdapter>()
//
//        val materialdialogpiece = MaterialDialog.Builder(activity)
//            .customView(R.layout.default_recycler_grey_200, false)
//            .negativeText(R.string.cancel)
//            .onPositive { _, _ ->
//                val map2 = FormSaveAndHook.saveData(fastFA2, mutableListOf())
//                Logger.d(map2)
//
//                val urltmp = map2["url"] as? String ?: ""
//                val title = map2["name"] as? String ?: ""
//
//                if (isInEditingMode && item != null) {
//                    val updatedTask = Site(title, urltmp, currentTime(), item.tas.id)
//                    mViewModel.updateTask(updatedTask)
//                    theList.remove(item)
//                    theList.add(BottomSheetCardItem(updatedTask, item.lastDiff))
//                    updatingCultos.update(theList)
//                } else {
//                    val url = when {
//                        !urltmp.startsWith("http://") && !urltmp.startsWith("https://") -> "http://$urltmp"
//                        else -> urltmp
//                    }
//
//                    val site = mViewModel.saveTask(title, url, currentTime())
//                    // add and sort the card
//                    theList.add(BottomSheetCardItem(site, null))
//                    updatingCultos.update(theList)
//                }
//            }
//
//        when (isInEditingMode) {
//            true ->
//                materialdialogpiece
//                    .title("Edit")
//                    .positiveText("Save")
//            false ->
//                materialdialogpiece
//                    .title("Add")
//                    .positiveText("Save")
//        }
//
//        val materialdialog = materialdialogpiece.build()
//
//        fastFA2.apply {
//            add(FormDecSection("Url", FormConstants.url, true))
//            add(FormDecSingleText(item?.tas?.url ?: "", FormConstants.url))
//
//            add(FormDecSection("Title (optional)", FormConstants.iname, true))
//            add(FormDecSingleText(item?.tas?.title ?: "", FormConstants.iname))
//        }
//
//        materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
//            layoutManager = LinearLayoutManager(this.context)
//            adapter = fastFA2
//        }
//
//        materialdialog.show()
//    }

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

        fun obtainViewModel(activity: FragmentActivity): TasksViewModel {
            // Use a Factory to inject dependencies into the ViewModel
            val factory = ViewModelFactory.getInstance(activity.application)
            return ViewModelProviders.of(activity, factory).get(TasksViewModel::class.java)
        }
    }
}

