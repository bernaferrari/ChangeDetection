package com.bernaferrari.changedetection.detailstext

import android.annotation.TargetApi
import android.arch.lifecycle.Observer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.transition.AutoTransition
import android.support.transition.ChangeBounds
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.bernaferrari.changedetection.MainActivity
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ScopedFragment
import com.bernaferrari.changedetection.ViewModelFactory
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.groupie.TextRecycler
import com.bernaferrari.changedetection.ui.ElasticDragDismissFrameLayout
import com.bernaferrari.changedetection.ui.RecyclerViewItemListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.control_bar.*
import kotlinx.android.synthetic.main.control_bar_diff.*
import kotlinx.android.synthetic.main.diff_text_fragment.*
import kotlinx.android.synthetic.main.state_layout.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class TextFragment : ScopedFragment() {
    lateinit var model: TextViewModel
    val color: Int by lazy { ContextCompat.getColor(requireContext(), R.color.FontStrong) }

    private val uiState = UiState { updateUiFromState() }
    private lateinit var bottomAdapter: TextAdapter
    private val topSection = Section()

    private val fsmCallback: ((String?, String?) -> Unit) = { str1, str2 ->
        model.generateDiff(topSection, str1, str2)

        when {
            uiState.diff -> launch(Dispatchers.Main) {
                val result = model.generateDiffVisual(str1, str2)
                putDataOnWebView(webview, result)
            }
            uiState.revised -> loadIntoWebView(true)
            uiState.original -> loadIntoWebView(false)
        }
    }

    private val canShowDiff: (() -> Unit) = {
        topSection.update(mutableListOf())
        webview.loadUrl("about:blank")
    }

    private val recyclerListener = object :
        RecyclerViewItemListener {
        override fun onClickListener(item: RecyclerView.ViewHolder) {
            if (item !is TextViewHolder) return
            stateLayout.showLoading()
            model.fsmSelectWithCorrectColor(item, fsmCallback)
            model.updateCanShowDiff(item.adapter, canShowDiff)
        }

        override fun onLongClickListener(item: RecyclerView.ViewHolder) {
            if (item !is TextViewHolder) return

            MaterialDialog(requireContext())
                .title(text = getString(R.string.remove_this, item.readableFileSize))
                .message(
                    text =
                    getString(
                        R.string.are_you_sure,
                        item.readableFileSize,
                        item.stringFromTimeAgo
                    )
                )
                .negativeButton(R.string.cancel)
                .positiveButton(R.string.yes) { _ ->
                    if (item.colorSelected != ItemSelected.NONE) {
                        // If the item is selected, first deselect, then remove it.
                        model.fsmSelectWithCorrectColor(item, fsmCallback)
                        model.updateCanShowDiff(item.adapter, canShowDiff)
                    }

                    model.removeSnap(item.snap?.snapId)
                }
                .show()
        }
    }

    private val transitionDelay = 175L
    private val transition = AutoTransition().apply { duration = transitionDelay }
    private fun beginDelayedTransition() =
        TransitionManager.beginDelayedTransition(elastic, transition)

    private fun updateUiFromState() {
        beginDelayedTransition()
        bottomRecycler.isVisible = uiState.visibility

        topRecycler.isVisible = uiState.sourceView
        sourceView.isActivated = uiState.sourceView
        showOriginalAndChanges.isVisible = uiState.sourceView

        webview.isVisible = !uiState.sourceView
        controlBarDiff.isVisible = !uiState.sourceView

        diff.isActivated = uiState.diff
        originalToggle.isActivated = uiState.original
        revisedToggle.isActivated = uiState.revised

        if (uiState.showOriginalAndChanges != showOriginalAndChanges.isActivated) {
            showOriginalAndChanges.isActivated = uiState.showOriginalAndChanges

            model.changePlusOriginal = !model.changePlusOriginal

            trySilently {
                // Don't do anything. If this exception happened, is because there are not
                // two items selected. So it won't change anything.

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
    ): View = inflater.inflate(R.layout.diff_text_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model = viewModelProvider(ViewModelFactory.getInstance(requireActivity().application))

        closecontent.setOnClickListener { dismiss() }
        titlecontent.text = getStringFromArguments(MainActivity.TITLE).takeIf { it.isNotBlank() }
                ?: getStringFromArguments(MainActivity.URL)

        elastic.addListener(object : ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            override fun onDragDismissed() {
                dismiss()
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

        sourceView.setOnClickListener {
            // disable touch on recyclerview to avoid crash when animation happens
            topRecycler.stopScroll()
            topRecycler.setOnTouchListener { _, _ -> true }
            uiState.sourceView++
            launch(Dispatchers.Main) {
                delay(transitionDelay + 50)
                topRecycler.setOnTouchListener { _, _ -> false }
            }
        }

        highQualityToggle.isVisible = false
        shareToggle.isVisible = false

        showOriginalAndChanges.isActivated = model.changePlusOriginal
        uiState.showOriginalAndChanges = model.changePlusOriginal
        showOriginalAndChanges.setOnClickListener { uiState.showOriginalAndChanges++ }

        openBrowserToggle.setOnClickListener {
            context?.openInBrowser(getStringFromArguments(MainActivity.URL))
        }

        closecontent.setOnClickListener { dismiss() }

        topRecycler.apply {
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

        // Create adapter for the RecyclerView
        bottomAdapter = TextAdapter(recyclerListener)

        bottomRecycler.run {
            adapter = bottomAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = itemAnimatorWithoutChangeAnimations()
        }

        fetchData()

        revisedToggle.setOnClickListener {
            uiState.revised = true
            uiState.diff = false
            uiState.original = false
            loadIntoWebView(true)
        }

        originalToggle.setOnClickListener {
            uiState.original = true
            uiState.revised = false
            uiState.diff = false
            loadIntoWebView(false)
        }

        diff.setOnClickListener {
            uiState.diff = true
            uiState.original = false
            uiState.revised = false
            fetchAndShow()
        }
    }

    private fun fetchAndShow() = launch(Dispatchers.Main) {
        val result = model.generateDiffVisual(
            originalId = bottomAdapter.getItemFromAdapter(1)?.snapId,
            revisedId = bottomAdapter.getItemFromAdapter(0)?.snapId
        )
        putDataOnWebView(webview, result)
    }

    private var loadIntoWebViewJob: Job? = null

    private fun loadIntoWebView(revised: Boolean) {
        loadIntoWebViewJob?.cancel()
        loadIntoWebViewJob = launch(Dispatchers.Main) {
            val color = if (revised) ItemSelected.REVISED else ItemSelected.ORIGINAL

            bottomAdapter.colorSelected.getPositionForAdapter(color)
                ?.let { bottomAdapter.getItemFromAdapter(it)?.snapId }
                ?.let { model.getSnapValue(it) }
                ?.also {
                    putDataOnWebView(
                        webview,
                        it.replaceRelativePathWithAbsolute(getStringFromArguments(MainActivity.URL))
                    )
                }
        }
    }

    private fun fetchData() = launch(Dispatchers.Main) {

        // Subscribe the adapter to the ViewModel, so the items in the adapter are refreshed
        // when the list changes
        var hasSetInitialColor = false

        val liveData = model.getAllSnapsPagedForId(
            getStringFromArguments(MainActivity.SITEID),
            getStringFromArguments(MainActivity.TYPE, "%")
        )

        liveData.observe(this@TextFragment, Observer {
            bottomAdapter.submitList(it)
            if (!hasSetInitialColor) {
                bottomAdapter.setColor(ItemSelected.REVISED, 0)
                bottomAdapter.setColor(ItemSelected.ORIGINAL, 1)

                fetchAndShow()

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
                    dismiss()
                }

                hasSetInitialColor = true
            }
        })
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
        var sourceView by BooleanProperty(false)
        var diff by BooleanProperty(true)
        var revised by BooleanProperty(false)
        var original by BooleanProperty(false)
    }
}
