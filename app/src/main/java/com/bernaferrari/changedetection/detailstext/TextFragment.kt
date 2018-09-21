package com.bernaferrari.changedetection.detailstext

import android.annotation.TargetApi
import android.arch.lifecycle.Observer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.transition.ChangeBounds
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.MainActivity
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ScopedFragment
import com.bernaferrari.changedetection.ViewModelFactory
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.groupie.DialogItemSimple
import com.bernaferrari.changedetection.groupie.TextRecycler
import com.bernaferrari.changedetection.ui.CustomWebView
import com.bernaferrari.changedetection.ui.ElasticDragDismissFrameLayout
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.content_web.view.*
import kotlinx.android.synthetic.main.control_bar.*
import kotlinx.android.synthetic.main.details_fragment.*
import kotlinx.android.synthetic.main.recyclerview.view.*
import kotlinx.android.synthetic.main.state_layout.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class TextFragment : ScopedFragment() {
    lateinit var model: TextViewModel
    val color: Int by lazy { ContextCompat.getColor(requireContext(), R.color.FontStrong) }

    private val uiState = UiState { updateUiFromState() }
    private lateinit var bottomAdapter: TextAdapter
    private val topSection = Section()

    private fun updateUiFromState() {

        bottomRecycler.isVisible = uiState.visibility

        if (uiState.showOriginalAndChanges != showOriginalAndChanges.isActivated) {
            showOriginalAndChanges.isActivated = uiState.showOriginalAndChanges

            model.changePlusOriginal = !model.changePlusOriginal

            try {
                val posRevised =
                    bottomAdapter.colorSelected.getPositionForAdapter(ItemSelected.REVISED)
                            ?: return
                val posOriginal =
                    bottomAdapter.colorSelected.getPositionForAdapter(ItemSelected.ORIGINAL)
                            ?: return

                model.generateDiff(
                    topSection = topSection,
                    originalId = bottomAdapter.getItemFromAdapter(posOriginal)?.snapId,
                    revisedId = bottomAdapter.getItemFromAdapter(posRevised)?.snapId
                )
            } catch (e: IllegalStateException) {
                // Don't do anything. If this exception happened, is because there are not
                // two items selected. So it won't change anything.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = ChangeBounds().apply { duration = MainActivity.TRANSITION }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.details_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model = viewModelProvider(ViewModelFactory.getInstance(requireActivity().application))

        closecontent.setOnClickListener { dismiss() }
        titlecontent.text = getStringFromArguments(MainActivity.TITLE).takeIf { it.isNotBlank() }
                ?: getStringFromArguments(MainActivity.URL)

        elastic.addListener(object : ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            override fun onDragDismissed() {
                view.findNavController().navigateUp()
            }
        })

        stateLayout.showLoading()

        model.showNotEnoughInfoError.observe(this, Observer {
            if (it == true) {
                stateLayout.setEmptyText(R.string.empty_please_select_two)
                stateLayout.showEmptyState()
            }
        })

        model.showNoChangesDetectedError.observe(this, Observer {
            stateLayout.setEmptyText(R.string.empty_no_change_detected)
            stateLayout.showEmptyState()
        })

        model.showProgress.observe(this, Observer {
            stateLayout.showLoading()
        })

        highQualityToggle.isVisible = false
        shareToggle.isVisible = false

        showOriginalAndChanges.isActivated = model.changePlusOriginal
        uiState.showOriginalAndChanges = model.changePlusOriginal
        showOriginalAndChanges.setOnClickListener { uiState.showOriginalAndChanges++ }

        openBrowserToggle.setOnClickListener {
            context?.openInBrowser(getStringFromArguments(MainActivity.URL))
        }

        closecontent.setOnClickListener { dismiss() }

        settings.isVisible = getStringFromArguments(MainActivity.TYPE) == "text/html"

        topRecycler.apply {
            layoutManager = LinearLayoutManager(context)

            setEmptyView(stateLayout)

            adapter = GroupAdapter<ViewHolder>().apply {
                add(topSection)
                setOnItemClickListener { item, _ ->
                    if (item is TextRecycler) {
                        copyToClipboard(context, item.title)
                    }
                }
            }
        }

        val recyclerListener = object :
            RecyclerViewItemListener {
            override fun onClickListener(item: RecyclerView.ViewHolder) {
                if (item !is TextViewHolder) return
                stateLayout.showLoading()
                model.fsmSelectWithCorrectColor(item, topSection)
            }

            override fun onLongClickListener(item: RecyclerView.ViewHolder) {
                if (item !is TextViewHolder) return

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
                        if (item.colorSelected != ItemSelected.NONE) {
                            // If the item is selected, first deselect, then remove it.
                            model.fsmSelectWithCorrectColor(item, topSection)
                        }

                        model.removeSnap(item.snap?.snapId)
                    }
                    .show()
            }
        }

        // Create adapter for the RecyclerView
        bottomAdapter = TextAdapter(recyclerListener)

        bottomRecycler.run {
            this.adapter = bottomAdapter
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

        launch(Dispatchers.Default) {

            val liveData = model.getAllSnapsPagedForId(
                getStringFromArguments(MainActivity.SITEID),
                getStringFromArguments(MainActivity.TYPE, "%")
            )

            withContext(Dispatchers.Main) {
                liveData.observe(this@TextFragment, Observer {
                    bottomAdapter.submitList(it)
                    if (!hasSetInitialColor) {
                        bottomAdapter.setColor(ItemSelected.REVISED, 0)
                        bottomAdapter.setColor(ItemSelected.ORIGINAL, 1)

                        try {
                            model.generateDiff(
                                topSection = topSection,
                                originalId = bottomAdapter.getItemFromAdapter(1)?.snapId,
                                revisedId = bottomAdapter.getItemFromAdapter(0)?.snapId
                            )
                        } catch (e: Exception) {
                            Snackbar.make(
                                elastic,
                                getString(R.string.less_than_two),
                                Snackbar.LENGTH_LONG
                            ).show()
                            view.findNavController().navigateUp()
                        }

                        hasSetInitialColor = true
                    }
                })
            }
        }

        settings.run {
            setImageDrawable(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_dots_vertical)
                    .color(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.dark_icon
                        )
                    )
                    .color(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.dark_icon
                        )
                    )
                    .sizeDp(18)
            )

            setOnClickListener { _ ->

                val customView =
                    layoutInflater.inflate(R.layout.recyclerview, view as ViewGroup, false)

                val materialdialog = BottomSheetDialog(requireContext())
                materialdialog.setContentView(customView)

                val updating = mutableListOf<Item<out ViewHolder>>()

                updating += DialogItemSimple(
                    getString(R.string.open_revised_in_browser),
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_vector_difference_ba).color(
                        ContextCompat.getColor(
                            context,
                            R.color.md_green_500
                        )
                    ),
                    "first"
                )

                updating += DialogItemSimple(
                    getString(R.string.open_original_in_browser),
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_vector_difference_ab).color(
                        ContextCompat.getColor(
                            context,
                            R.color.md_red_500
                        )
                    ),
                    "second"
                )

                val groupAdapter = GroupAdapter<ViewHolder>().apply {
                    add(Section(updating))
                }

                customView?.defaultRecycler?.run {
                    this.adapter = groupAdapter
                    this.layoutManager = LinearLayoutManager(requireContext())
                }

                groupAdapter.setOnItemClickListener { itemDialog, _ ->
                    if (itemDialog is DialogItemSimple) {
                        MaterialDialog.Builder(context)
                            .customView(R.layout.content_web, false)
                            .negativeText(R.string.close)
                            .build()
                            .also { dialog ->
                                dialog.customView?.also { view ->
                                    view.webview.updateLayoutParams {
                                        this.height = resources.displayMetrics.heightPixels
                                        this.width = resources.displayMetrics.widthPixels
                                    }

                                    fetchAndOpenOnWebView(
                                        bottomAdapter,
                                        view.webview,
                                        if (itemDialog.kind == "first")
                                            ItemSelected.REVISED
                                        else
                                            ItemSelected.ORIGINAL
                                    )
                                }
                            }.show()
                    }
                }

                materialdialog.show()
            }
        }
    }

    private fun fetchAndOpenOnWebView(
        adapter: TextAdapter,
        view: CustomWebView,
        color: ItemSelected
    ) = launch(Dispatchers.Default) {
        adapter.colorSelected.getPositionForAdapter(color)
            ?.let { adapter.getItemFromAdapter(it)?.snapId }
            ?.let { model.getSnapValue(it) }
            ?.also {
                withContext(Dispatchers.Main) {
                    putDataOnWebView(
                        view,
                        it.replaceRelativePathWithAbsolute(
                            getStringFromArguments(MainActivity.URL)
                        )
                    )
                }
            }
    }

    private fun copyToClipboard(context: Context, uri: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.app_name), uri)

        clipboard.primaryClip = clip
        Snackbar.make(elastic, getString(R.string.success_copied), Snackbar.LENGTH_SHORT).show()
    }

    private fun putDataOnWebView(webView: WebView?, data: String) {
        webView?.loadDataWithBaseURL("", data, "text/html", "UTF-8", "")
    }

    private fun dismiss() = view?.findNavController()?.navigateUp()

    companion object {
        interface RecyclerViewItemListener {
            fun onClickListener(item: RecyclerView.ViewHolder)
            fun onLongClickListener(item: RecyclerView.ViewHolder)
        }
    }

    private class UiState(private val callback: () -> Unit) {

        private inner class BooleanProperty(initialValue: Boolean) :
            ObservableProperty<Boolean>(initialValue) {
            override fun afterChange(
                property: KProperty<*>,
                oldValue: Boolean,
                newValue: Boolean
            ) {
                callback()
            }
        }

        var visibility by BooleanProperty(true)
        var showOriginalAndChanges by BooleanProperty(false)
    }
}
