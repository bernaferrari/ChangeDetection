package com.bernaferrari.changedetection

import android.annotation.TargetApi
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.Navigation
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.extensions.getPositionForAdapter
import com.bernaferrari.changedetection.groupie.DialogItemSimple
import com.bernaferrari.changedetection.groupie.DialogItemSwitch
import com.bernaferrari.changedetection.groupie.TextRecycler
import com.bernaferrari.changedetection.ui.ElasticDragDismissFrameLayout
import com.bernaferrari.changedetection.ui.PrettifyWebView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.details_fragment.*
import kotlinx.android.synthetic.main.state_layout.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class DetailsFragment : Fragment() {
    lateinit var model: DetailsViewModel
    val color: Int by lazy { ContextCompat.getColor(requireContext(), R.color.FontStrong) }

    interface RecyclerViewItemListener {
        fun onClickListener(item: RecyclerView.ViewHolder)
        fun onLongClickListener(item: RecyclerView.ViewHolder)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model = obtainViewModel(requireActivity())

        closecontent.setOnClickListener { dismiss() }
        titlecontent.text = arguments?.getString(TITLE) ?: ""

        this.elastic.addListener(object :
            ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            override fun onDragDismissed() {
                view?.let { Navigation.findNavController(it).navigateUp() }
            }
        })

        val state = stateLayout
        state.apply {
            showLoading()
        }

        val topSection = Section()

        model.showNotEnoughtInfoError.observe(this, Observer {
            if (it == true) {
                state.setEmptyText(R.string.empty_please_select_two)
                state.showEmptyState()
            }
        })

        model.showNoChangesDetectedError.observe(this, Observer {
            state.setEmptyText(R.string.empty_no_change_detected)
            state.showEmptyState()
        })

        model.showProgress.observe(this, Observer {
            state.showLoading()
        })

        val siteId = arguments?.getString(SITEID) ?: ""

        topRecycler.run {
            layoutManager = LinearLayoutManager(context)

            setEmptyView(state)

            adapter = GroupAdapter<ViewHolder>().apply {
                add(topSection)
                setOnItemClickListener { item, _ ->
                    if (item !is TextRecycler) return@setOnItemClickListener
                    copyToClipboard(context, item.title)
                }
            }
        }

        val recyclerListener = object : RecyclerViewItemListener {
            override fun onClickListener(item: RecyclerView.ViewHolder) {
                if (item !is DetailsViewHolder) return
                state.showLoading()
                model.fsmSelectWithCorrectColor(item, topSection)
            }

            override fun onLongClickListener(item: RecyclerView.ViewHolder) {
                if (item !is DetailsViewHolder) return

                MaterialDialog.Builder(requireActivity())
                    .title(getString(R.string.remove_this, item.readableFileSize))
                    .content(
                        getString(
                            R.string.are_you_sure,
                            item.readableFileSize,
                            item.stringFromTimeAgo
                        )
                    )
                    .negativeText(R.string.cancel)
                    .positiveText(R.string.yes)
                    .onPositive { _, _ ->
                        if (item.colorSelected != 0) {
                            // If the item is selected, first deselect, then remove it.
                            model.fsmSelectWithCorrectColor(item, topSection)
                        }
                        model.removeSnap(item.minimalSnap!!.snapId)

                    }
                    .show()
            }
        }

        // Create adapter for the RecyclerView
        val adapter = DetailsAdapter(recyclerListener)

        bottomRecycler.run {
            this.adapter = adapter
            this.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = this.itemAnimator.apply {
                // From https://stackoverflow.com/a/33302517/4418073
                if (this is SimpleItemAnimator) {
                    this.supportsChangeAnimations = false
                }
            }
        }

        // Subscribe the adapter to the ViewModel, so the items in the adapter are refreshed
        // when the list changes
        var hasSetInitialColor = false
        model.getWebHistoryForId(siteId).observe(this, Observer {
            adapter.submitList(it)
            if (!hasSetInitialColor) {
                adapter.setColor(2, 0)
                adapter.setColor(1, 1)

                try {
//                    (adapter.getItemFromAdapter(0) != null || adapter.getItemFromAdapter(1) != null)
                    model.generateDiff(
                        topSection,
                        adapter.getItemFromAdapter(1)!!.snapId,
                        adapter.getItemFromAdapter(0)!!.snapId
                    )
                } catch (e: Exception) {
                    Toasty.error(
                        requireContext(),
                        getString(R.string.less_than_two),
                        Toast.LENGTH_LONG
                    ).show()
                    view?.let { Navigation.findNavController(it).navigateUp() }
                }

                hasSetInitialColor = true
            }
        })

        closecontent.setOnClickListener {
            dismiss()
        }

        settings.run {
            setImageDrawable(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_dots_vertical)
                    .color(ContextCompat.getColor(requireContext(), R.color.dark_icon))
                    .color(ContextCompat.getColor(requireContext(), R.color.dark_icon))
                    .sizeDp(18)
            )

            setOnLongClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        (arguments?.getString(URL) ?: "http://").toUri()
                    )
                )
                true
            }

            setOnClickListener {
                val materialdialog = MaterialDialog.Builder(this.context)
                    .customView(R.layout.recyclerview, false)
                    .build()

                val updating = mutableListOf<Item<out ViewHolder>>()

                updating += DialogItemSwitch(
                    getString(R.string.original_plus_diffs),
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_vector_difference).color(
                        ContextCompat.getColor(context, R.color.md_indigo_500)
                    ),
                    model.changePlusOriginal
                ) {
                    model.changePlusOriginal = !model.changePlusOriginal

                    try {
                        val pos1 = adapter.colorSelected.getPositionForAdapter(1)
                        val pos2 = adapter.colorSelected.getPositionForAdapter(2)

                        model.generateDiff(
                            topSection,
                            adapter.getItemFromAdapter(pos1!!)!!.snapId,
                            adapter.getItemFromAdapter(pos2!!)!!.snapId
                        )
                    } catch (e: Exception) {
                        // Don't do anything. If this exception happened, is because there are not
                        // two items selected. So it won't change anything.
                    }

                    // Dismiss after the user taps on the switch, but not before he sees the animation
                    launch {
                        delay(250) // non-blocking delay for 250 ms
                        launch(UI) {
                            materialdialog.dismiss()
                        }
                    }
                }

                updating += DialogItemSimple(
                    getString(R.string.open_1_in_browser),
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_vector_difference_ba).color(
                        ContextCompat.getColor(context, R.color.md_amber_500)
                    ),
                    "first"
                )

                updating += DialogItemSimple(
                    getString(R.string.open_2_in_browser),
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_vector_difference_ab).color(
                        ContextCompat.getColor(context, R.color.md_orange_500)
                    ),
                    "second"
                )

                val groupAdapter = GroupAdapter<ViewHolder>().apply {
                    add(Section(updating))
                }

                materialdialog.customView?.findViewById<RecyclerView>(R.id.defaultRecycler)?.run {
                    this.adapter = groupAdapter
                    this.layoutManager = LinearLayoutManager(requireContext())
                }

                groupAdapter.setOnItemClickListener { itemDialog, _ ->
                    if (itemDialog is DialogItemSimple) {
                        when (itemDialog.kind) {
                            "first" -> {
                                MaterialDialog.Builder(context)
                                    .customView(R.layout.content_web, false)
                                    .build()
                                    .also {
                                        fetchAndOpenOnWebView(adapter, it, 1)
                                    }.show()
                            }
                            "second" -> {
                                MaterialDialog.Builder(context)
                                    .customView(R.layout.content_web, false)
                                    .build()
                                    .also {
                                        fetchAndOpenOnWebView(adapter, it, 2)
                                    }.show()
                            }
                        }
                    }
                }

                materialdialog.show()
            }
        }
    }

    private fun fetchAndOpenOnWebView(adapter: DetailsAdapter, dialog: MaterialDialog, color: Int) {
        val position = adapter.colorSelected.getPositionForAdapter(color) ?: return

        launch {
            val snap =
                model.getSnap(
                    adapter.getItemFromAdapter(
                        position
                    )?.snapId ?: return@launch
                )

            launch(UI) {
                putDataOnWebView(
                    dialog.customView?.findViewById<PrettifyWebView>(
                        R.id.webview
                    ),
                    snap.value
                )
            }
        }
    }

    private fun copyToClipboard(context: Context, uri: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.app_name), uri)

        clipboard.primaryClip = clip
        Toasty.success(context, context.getString(R.string.success_copied)).show()
    }

    private fun putDataOnWebView(webView: WebView?, data: String) {
        webView?.loadDataWithBaseURL("", data, "text/html", "UTF-8", "")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.details_fragment, container, false)
    }

    private fun dismiss() {
        view?.let { Navigation.findNavController(it).navigateUp() }
//        activity?.supportFragmentManager?.popBackStack()
    }

    private fun obtainViewModel(activity: FragmentActivity): DetailsViewModel {
        // Use a Factory to inject dependencies into the ViewModel
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProviders.of(activity, factory).get(DetailsViewModel::class.java)
    }

    companion object {
        private val SITEID = "SITEID"
        private val TITLE = "TITLE"
        private val URL = "URL"

        fun newInstance(id: String, title: String, url: String): DetailsFragment {
            return DetailsFragment().apply {
                arguments = Bundle(3).apply {
                    putString(SITEID, id)
                    putString(TITLE, title)
                    putString(URL, url)
                }
            }
        }
    }
}
