package com.example.changedetection

import android.annotation.TargetApi
import android.arch.lifecycle.Observer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.databinding.Observable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.BottomSheetDialog
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.navigation.Navigation
import com.afollestad.materialdialogs.MaterialDialog
import com.example.changedetection.groupie.DialogItem
import com.example.changedetection.groupie.DialogItemSwitch
import com.example.changedetection.groupie.DiffItem
import com.example.changedetection.groupie.TextRecycler
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import es.dmoral.toasty.Toasty
import io.reactivex.disposables.Disposables
import kotlinx.android.synthetic.main.default_recycler_dialog.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class OpenFragment : Fragment() {
    lateinit var model: TasksViewModel
    val color: Int by lazy { ContextCompat.getColor(requireContext(), R.color.FontStrong) }

    private inline fun <T: Observable> T.addOnPropertyChanged(crossinline callback: (T) -> Unit) =
        object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable?, i: Int) = callback(observable as T)
        }
            .also { addOnPropertyChangedCallback(it) }
            .let { Disposables.fromAction { removeOnPropertyChangedCallback(it) } }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model = MainActivity.obtainViewModel(requireActivity())

        closecontent.setOnClickListener { dismiss() }
        titlecontent.text = arguments?.getString(TITLE) ?: ""

        this.elastic.addListener(object : ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            override fun onDragDismissed() {
                view?.let { Navigation.findNavController(it).navigateUp() }
            }
        })

        val sectionTop = Section()
        val sectionBottom = Section()
        val updatinghor = mutableListOf<DiffItem>()

        model.canShowDiff.observe(this, Observer {
            isempty.visibility = if (it == false) View.VISIBLE else View.GONE
        })

        model.getWebHistoryForId(arguments?.getString(TASKID) ?: "")
            .observe(this, Observer { diffList ->
                Logger.d("DiffList: $diffList")
                if (diffList != null) {
                    updatinghor.clear()
                    diffList.mapTo(updatinghor) { DiffItem(it) }
                    model.fromLiveData(updatinghor, sectionTop)
                    sectionBottom.update(updatinghor)
                }
            })

        topRecycler.run {
            layoutManager = LinearLayoutManager(context)
            adapter = GroupAdapter<ViewHolder>().apply {
                add(sectionTop)
                setOnItemClickListener { item, view ->
                    if (item !is TextRecycler) return@setOnItemClickListener
                    copyToClipboard(context, item.title)
                }
            }
        }

        bottomRecycler.run {
            itemAnimator = null
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = GroupAdapter<ViewHolder>().apply {
                add(sectionBottom)
                setOnItemClickListener { item, view ->
                    if (item !is DiffItem) return@setOnItemClickListener
                    model.onClick(item, updatinghor, sectionTop)
                }
            }
        }

        closecontent.setOnClickListener {
            dismiss()
        }

        settings.run {
            setImageDrawable(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_dots_vertical)
                    .color(ContextCompat.getColor(requireContext(), R.color.dark_icon))
                    .sizeDp(18)
            )

            setOnLongClickListener {
                startActivity(Intent(
                    Intent.ACTION_VIEW,
                    (arguments?.getString(URL) ?: "http://").toUri()
                ))
                true
            }

            setOnClickListener {
                val materialdialog = MaterialDialog.Builder(this.context)
                    .customView(R.layout.default_recycler_grey_200, false)
                    .build()

                val updating = mutableListOf<Item<out ViewHolder>>()

                updating += DialogItemSwitch(
                    "Original + Diffs",
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_vector_difference).color(
                        ContextCompat.getColor(context, R.color.md_indigo_500)
                    ),
                    "switch",
                    true,
                    model.withAllDiff
                ) {
                    model.withAllDiff = !model.withAllDiff
                    model.diffagain(
                        section = sectionTop,
                        original = updatinghor.firstOrNull { it.colorSelected == 1 }?.diff,
                        new = updatinghor.firstOrNull { it.colorSelected == 2 }?.diff
                    )

                    // Dismiss after the user taps on the switch, but not before he sees the animation
                    launch {
                        delay(250) // non-blocking delay for 250 ms
                        launch(UI) {
                            materialdialog.dismiss()
                        }
                    }
                }

                updating += DialogItem(
                    "Open #1 in Browser",
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_vector_difference_ba).color(
                        ContextCompat.getColor(context, R.color.md_amber_500)
                    ),
                    "first"
                )

                updating += DialogItem(
                    "Open #2 in Browser",
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_vector_difference_ab).color(
                        ContextCompat.getColor(context, R.color.md_orange_500)
                    ),
                    "second"
                )

                val groupAdapter = GroupAdapter<ViewHolder>().apply {
                    add(Section(updating))
                }

                materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
                    adapter = groupAdapter
                    layoutManager = LinearLayoutManager(requireContext())
                }

                groupAdapter.setOnItemClickListener { itemDialog, view ->
                    if (itemDialog is DialogItem) {
                        when (itemDialog.kind) {
                            "first" -> {
                                MaterialDialog.Builder(context)
                                    .customView(R.layout.content_web, false)
                                    .build()
                                    .also {
                                        putDataOnWebView(
                                            it.customView?.findViewById<PrettifyWebView>(R.id.webview),
                                            updatinghor.firstOrNull { it.colorSelected == 1 }?.diff?.value ?: ""
                                        )
                                    }.show()
                            }
                            "second" -> {
                                MaterialDialog.Builder(context)
                                    .customView(R.layout.content_web, false)
                                    .build()
                                    .also {
                                        putDataOnWebView(
                                            it.customView?.findViewById<PrettifyWebView>(R.id.webview),
                                            updatinghor.firstOrNull { it.colorSelected == 2 }?.diff?.value ?: ""
                                        )
                                    }.show()
                            }
                        }
                    }
                }

                materialdialog.show()
            }
        }
    }

    private fun copyToClipboard(context: Context, uri: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.app_name), uri)

        clipboard.primaryClip = clip
        Toasty.success(context, context.getString(R.string.success_copied)).show()
    }

    private fun putDataOnWebView(webView: WebView?, data: String){
        webView?.loadDataWithBaseURL("", data, "text/html", "UTF-8", "");
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.default_recycler_dialog, container, false)
    }

    private fun dismiss() {
        view?.let { Navigation.findNavController(it).navigateUp() }
//        activity?.supportFragmentManager?.popBackStack()
    }

    companion object {
        private val TASKID = "TASKID"
        private val TITLE = "TITLE"
        private val URL = "URL"

        fun newInstance(id: String, title: String, url: String): OpenFragment {
            return OpenFragment().apply {
                arguments = Bundle(3).apply {
                    putString(TASKID, id)
                    putString(TITLE, title)
                    putString(URL, url)
                }
            }
        }
    }
}
