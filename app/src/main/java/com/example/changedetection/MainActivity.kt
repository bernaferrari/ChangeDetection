package com.example.changedetection

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.example.changedetection.data.Diff
import com.example.changedetection.data.Task
import com.example.changedetection.diffs.text.DiffRowGenerator
import com.example.changedetection.forms.*
import com.example.changedetection.groupie.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.abc_alert_dialog_material.*
import kotlinx.android.synthetic.main.empty_layout.view.*
import kotlinx.android.synthetic.main.state_layout.*
import kotlinx.android.synthetic.main.todos_encontros_activity.*
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {
    private lateinit var mViewModel: TasksViewModel
    private val groupAdapter = GroupAdapter<ViewHolder>()
    private var theList = mutableListOf<Item>()
    private var updatingCultos = Section(theList)

    val color: Int by lazy { ContextCompat.getColor(this, R.color.FontStrong) }

    fun dp(value: Int, resources: Resources): Int {
        return (resources.displayMetrics.density * value).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.todos_encontros_activity)

        mViewModel = obtainViewModel(this)

        val compressionWork = OneTimeWorkRequest.Builder(SyncWorker::class.java).build()
        WorkManager.getInstance().enqueue(compressionWork)

        val div = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        div.setDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.divider2)!!)

        fab.background = IconicsDrawable(this, CommunityMaterial.Icon.cmd_plus)
        fab.setOnClickListener {
            criarEditarDialog(false, this)
        }

        stateLayout.empty_text.text = "Nothing found"
        stateLayout.showEmptyState()

        groupAdapter.apply {
            spanCount = calculateNoOfColumns(this@MainActivity)
        }

        defaultRecycler.apply {
            this.setEmptyView(stateLayout)
            addItemDecoration(div)
            adapter = groupAdapter
            layoutManager = GridLayoutManager(this.context, groupAdapter.spanCount).apply {
                spanSizeLookup = groupAdapter.spanSizeLookup
            }
        }

        groupAdapter.add(MarqueeItem("Change Detection"))
        groupAdapter.add(updatingCultos)
        groupAdapter.setOnItemLongClickListener { item, view ->
            if (item is BottomSheetCardItem){
                criarEditarDialog(true, this, item as? BottomSheetCardItem)
                return@setOnItemLongClickListener true
            }
            return@setOnItemLongClickListener false
        }

        groupAdapter.setOnItemClickListener { item, view ->

            Logger.d("Getting inside task id: " + (item as? BottomSheetCardItem)?.tas?.id)

            if (item is BottomSheetCardItem){
                supportFragmentManager.run {
                    beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_up2,
                            R.anim.design_bottom_sheet_slide_out,
                            R.anim.slide_up2,
                            R.anim.design_bottom_sheet_slide_out
                        )
                        .add(
                            R.id.framelayout,
                            OpenFragment.newInstance(item.tas.id, item.title),
                            "elasticsheet"
                        )
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                }
            }
        }

        mViewModel.loadTasks(false)

        mViewModel.items2.observe(this, Observer {
            theList.clear()
            it?.forEach {
                theList.add(BottomSheetCardItem(it.task, it.diff))
            }
            updatingCultos.update(theList)
        })
    }

    fun generateDiff2(original: Diff?, it: Diff?): Pair<MutableList<TextRecycler>, MutableList<TextRecycler>> {
        if (original == null || it == null){
            Logger.d("original or it are null")
            return Pair(mutableListOf(), mutableListOf())
        }

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { f -> "TEXTREMOVED" }
            .newTag { f -> "TEXTADDED" }
            .build()

        //compute the differences for two test texts.
        val rows = generator.generateDiffRows(
            it.value.split("\n"),
            original.value.split("\n")
        )

        val updatingNonDiff = mutableListOf<TextRecycler>()
        val updatingOnlyDiff = mutableListOf<TextRecycler>()

        rows.forEachIndexed { index, row ->
            if (row.oldLine == row.newLine) {
                updatingNonDiff.add(TextRecycler(row.oldLine, index))
                println("$index none: " + row.oldLine)
            } else {
                when {
                    row.newLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine, index))
                        println("$index old: " + row.oldLine)
                    }
                    row.oldLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine, index))
                        println("$index new: " + row.newLine)
                    }
                    else -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine, index))
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine, index))

                        println("$index old: " + row.oldLine)
                        println("$index new: " + row.newLine)
                    }
                }
            }
        }

        return Pair(updatingOnlyDiff, updatingNonDiff)
    }


    fun generateDiff(original: Diff, it: Diff) {
        val updating = mutableListOf<TextRecycler>()
        val (updatingNonDiff, updatingOnlyDiff) = generateDiff2(original, it)

        updating.addAll(updatingNonDiff)
        updating.addAll(updatingOnlyDiff)
        updating.sortBy { it.index }

        val section = Section(updating)

        val groupAdapter = GroupAdapter<ViewHolder>().apply {
            add(section)
        }

        val materialdialog = BottomSheetDialogExpanded(this)

        layoutInflater.inflate(R.layout.bottom_sheet_recycler, framelayout, false).apply {
            findViewById<ImageView>(R.id.closecontent).setOnClickListener {
                materialdialog.dismiss()
            }

            findViewById<ImageView>(R.id.settings).run {
                setImageDrawable(IconicsDrawable(context, CommunityMaterial.Icon.cmd_settings)
                    .color(ContextCompat.getColor(context, R.color.dark_icon))
                    .sizeDp(24)
                )

                setOnClickListener {
                    section.update(updatingOnlyDiff)
                }
            }

            findViewById<TextView>(R.id.titlecontent).text = "Diff"

            findViewById<RecyclerView>(R.id.defaultRecycler).run {
                adapter = groupAdapter
            }

            materialdialog.setContentView(this)
        }

        materialdialog.show()
    }


    private fun reload(item: BottomSheetCardItem?){
        if (item !is BottomSheetCardItem){
            return
        }

        item.status = 1
        item.notifyChanged()

        val client = OkHttpClient()

        Single.fromCallable {
            val request = Request.Builder()
                .url(item.tas.url)
                .build()

            val response1 = client.newCall(request).execute()

            val resp2 = response1.isSuccessful

            Logger.d("isSuccessful -> $resp2")
            Logger.d("header -> ${response1.headers()
            }")

            val str = response1.body()?.string()
            Logger.d("str -> $str")

            return@fromCallable str
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                Logger.d("onErrorReturn")
                return@onErrorReturn ""
            }
            .subscribe { it ->
                Logger.d("count -> $it")

//                item.size = it?.count() ?: 0
//                item.timestamp = currentTime()

                if (it?.count() == 0 || it == null){
                    item.status = 2
                    item.notifyChanged()
                } else {
                    item.status = 0
                    if (it.isNotBlank()){
                        val diff = Diff(it, currentTime(), item.tas.id)

                        mViewModel.saveWebsite(diff).observe(this, Observer {
                            if (it == true){
                                item.notifyChanged(it)
                            }
                        })
                    }
                }

                item.notifyChanged()
            }
    }

    private fun currentTime():Long = System.currentTimeMillis()

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

                if (isInEditingMode && item != null){
                    val updatedTask = Task(title, urltmp, currentTime(), item.tas.id)
                    mViewModel.updateTask(updatedTask)
                    theList.remove(item)
                    theList.add(BottomSheetCardItem(updatedTask, item.lastDiff))
                    updatingCultos.update(theList)
                } else {

                    val url = when {
                        !urltmp.startsWith("http://") && !urltmp.startsWith("https://") -> "http://$urltmp"
                        else -> urltmp
                    }

                    val task = mViewModel.saveTask(title, url, currentTime())
                    // add and sort the card
                    theList.add(BottomSheetCardItem(task, null))
                    updatingCultos.update(theList)
                }
            }

        when (isInEditingMode) {
            true ->
                materialdialogpiece
                    .title("Edit")
                    .positiveText("Save")
                    .neutralText("Remove")
                    .onNeutral { _, _ ->
                        if (item != null){
                            mViewModel.completeTask(item.tas, true)
                            mViewModel.clearCompletedTasks()
                            theList.remove(item)
                            updatingCultos.update(theList)
                        }
                    }
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

    fun obtainViewModel(activity: FragmentActivity): TasksViewModel {
        // Use a Factory to inject dependencies into the ViewModel
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProviders.of(activity, factory).get(TasksViewModel::class.java)
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

