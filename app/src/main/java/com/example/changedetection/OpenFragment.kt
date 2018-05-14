package com.example.changedetection

import android.annotation.TargetApi
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.changedetection.data.Diff
import com.example.changedetection.groupie.DialogItemSwitch
import com.example.changedetection.groupie.DiffItem
import com.example.changedetection.groupie.LoadingPlaidItem
import com.example.changedetection.groupie.TextRecycler
import com.example.changedetection.ui.StateLayout
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.default_recycler_dialog.view.*

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class OpenFragment : Fragment() {
    lateinit var draggableFrame: ElasticDragDismissFrameLayout
    lateinit var close: ImageView
    lateinit var titleBar: TextView
    lateinit var recycler: RecyclerView
    lateinit var state: StateLayout
    lateinit var model: TasksViewModel

    val color: Int by lazy {
        ContextCompat.getColor(
            this@OpenFragment.context!!,
            R.color.FontStrong
        )
    }
    val section = Section()

    val updatinghor = mutableListOf<DiffItem>()

//    private val onItemClickListener = OnItemClickListener { item, view ->
//        // This is a simple Finite State Machine
//        if (item !is DiffItem) {
//            return@OnItemClickListener
//        }
//
//        when (item.colorSelected) {
//            2 -> {
//                // ORANGE -> GREY
//                item.notifyChanged(0)
//            }
//            1 -> {
//                // AMBER -> GREY
//                item.notifyChanged(0)
//            }
//            else -> {
//                when (updatinghor.count { it.colorSelected > 0 }) {
//                    0 -> {
//                        // NOTHING IS SELECTED -> AMBER
//                        item.notifyChanged(1)
//                    }
//                    1 -> {
//                        // ONE THING IS SELECTED AND IT IS AMBER -> ORANGE
//                        // ONE THING IS SELECTED AND IT IS ORANGE -> AMBER
//                        when (updatinghor.firstOrNull { it.colorSelected > 0 }?.colorSelected) {
//                            2 -> {
//                                item.notifyChanged(1)
//                                diffagain(
//                                    section,
//                                    item.diff,
//                                    updatinghor.first { it.colorSelected == 2 }.diff
//                                )
//                            }
//                            else -> {
//                                item.notifyChanged(2)
//                                diffagain(
//                                    section,
//                                    updatinghor.first { it.colorSelected == 1 }.diff,
//                                    item.diff
//                                )
//                            }
//                        }
//                    }
//                    else -> {
//                        // TWO ARE SELECTED. UNSELECT THE ORANGE, SELECT ANOTHER THING.
//                        updatinghor.first { it.colorSelected >= 2 }.notifyChanged(0)
//                        diffagain(
//                            section,
//                            updatinghor.first { it.colorSelected == 1 }.diff,
//                            item.diff
//                        )
//                        item.notifyChanged(2)
//                    }
//                }
//            }
//        }
//    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        close.setOnClickListener { dismiss() }
        model = obtainViewModel(requireActivity())

        titleBar.text = arguments?.getString(TITLE) ?: ""

        val chromeFader =
            ElasticDragDismissFrameLayout.SystemChromeFader(activity as AppCompatActivity)
        draggableFrame.addListener(chromeFader)


        val updating = mutableListOf<DialogItemSwitch>()
        val context = this@OpenFragment.context

        val historySection = Section().apply {
            setHeader(LoadingPlaidItem())
        }

        val groupAdapter = GroupAdapter<ViewHolder>().apply {
            add(historySection)
            add(Section(updating))
        }

        Logger.d("taskId: " + arguments?.getString(TASKID))

        model.getWebHistoryForId(arguments?.getString(TASKID) ?: "")
            .observe(this, Observer { diffList ->
                Logger.d("DiffList: $diffList")

                if (diffList != null) {
                    diffList.mapTo(updatinghor) { DiffItem(it) }

                    view?.findViewById<RecyclerView>(R.id.bottomRecycler)?.apply {
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                        adapter = GroupAdapter<com.xwray.groupie.ViewHolder>().apply {
                            updatinghor.apply {
                                get(0).colorSelected = 1
                                get(1).colorSelected = 2
                            }
                            add(Section(updatinghor))
                            setOnItemClickListener { item, view ->

                            }
                        }
                    }

                    diffagain(section, diffList.getOrNull(0)!!, diffList.getOrNull(1))
                    val groupAdapter2 = GroupAdapter<ViewHolder>().apply {
                        add(section)
                    }

                    view?.findViewById<RecyclerView>(R.id.topRecycler)?.adapter = groupAdapter2
                }
            })


        view?.run {
            findViewById<RecyclerView>(R.id.bottomRecycler)?.adapter = groupAdapter

            findViewById<ImageView>(R.id.closecontent).setOnClickListener {
                dismiss()
            }

            findViewById<ImageView>(R.id.settings).run {
                setImageDrawable(
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_dots_vertical)
                        .color(ContextCompat.getColor(requireContext(), R.color.dark_icon))
                        .sizeDp(18)
                )

                setOnClickListener {
                    //                        section.update(updatingOnlyDiff)
                }
            }
        }
    }

    val updatingOnlyDiff = mutableListOf<TextRecycler>()
    val updatingNonDiff = mutableListOf<TextRecycler>()



    private fun updateSection(section: Section, withAllDiff: Boolean = false) {
        mutableListOf<TextRecycler>().run {
            if (withAllDiff) {
                addAll(updatingNonDiff)
            }
            addAll(updatingOnlyDiff)
            sortBy { it.index }
            section.update(this)
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.default_recycler_dialog, container, false)
        view.run {
            draggableFrame = this.elastic
            close = this.closecontent
            titleBar = this.titlecontent
            recycler = this.topRecycler
        }

        return view
    }

    fun obtainViewModel(activity: FragmentActivity): TasksViewModel {
        // Use a Factory to inject dependencies into the ViewModel
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProviders.of(activity, factory).get(TasksViewModel::class.java)
    }

    fun dismiss() {
        activity?.supportFragmentManager?.popBackStack()
    }

    companion object {
        private val TASKID = "TASKID"
        private val TITLE = "TITLE"

        fun newInstance(id: String, title: String): OpenFragment {
            return OpenFragment().apply {
                arguments = Bundle(1).apply {
                    putString(TASKID, id)
                    putString(TITLE, title)
                }
            }
        }
    }
}
