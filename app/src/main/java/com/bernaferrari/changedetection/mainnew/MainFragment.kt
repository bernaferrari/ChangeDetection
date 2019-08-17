package com.bernaferrari.changedetection.mainnew

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.fragmentViewModel
import com.bernaferrari.base.misc.toDp
import com.bernaferrari.base.mvrx.simpleController
import com.bernaferrari.changedetection.*
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.itemAnimatorWithoutChangeAnimations
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.ui.dagger.DaggerBaseToolbarFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainFragment : DaggerBaseToolbarFragment() {

    override val menuTitle = "Change Detection"

    private val mViewModel: MainViewModelNEW by fragmentViewModel()

    @Inject
    lateinit var mainViewModelFactory: MainViewModelNEW.Factory

    private val transitionDelay = 175L
    private val transition = AutoTransition().apply { duration = transitionDelay }

    override fun epoxyController() = simpleController(mViewModel) { state ->

        if (state.isLoading) {
            loadingRow { id("loading") }
        } else if (state.listOfItems.isEmpty()) {
            marquee {
                id("empty")
                title("Nothing is currently tracked")
            }
        }

        state.listOfItems.forEachIndexed { _, it ->

            MainCardItem_()
                .id(it.site.id)
                .site(it.site)
                .lastSnap(it.snap)
                .title(getTitle(it.site))
                .lastSyncStr(getLastSync(requireContext(), it, it.isSyncing))
                .lastDiffStr(getLastDiff(requireContext(), it.snap))
                .syncingNow(it.isSyncing)
                .onReload { _ ->
                    WorkerHelper.reloadSite(it.site, requireContext().applicationContext)
                }
                .onClick { v ->
                    openItem(v, it)
                }
                .onLongClick { v ->
                    v.findNavController().navigate(
                        R.id.action_mainFragment_to_mainLongPressSheet,
                        bundleOf("site" to it.site)
                    )
                    true
                }
                .addTo(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.sortAlphabetically = Injector.get().sharedPrefs().getBoolean("sortByName", false)

        recyclerView.apply {
            itemAnimator = itemAnimatorWithoutChangeAnimations()
            setPadding(16.toDp(resources))
        }

        WorkManager.getInstance(requireContext().applicationContext)
            .getWorkInfosByTagLiveData("singleEvent")
            .observe(this) { list ->
                mViewModel.workManagerObserver.accept(
                    list.filter {
                        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                    }
                )
            }

        viewContainer.inflateAddButton().setOnClickListener {
            it.findNavController().navigate(R.id.action_mainFragment_to_addNew)
        }

        configureToolbarFilters()
    }

    private fun configureToolbarFilters() {

        val colorRecycler = requireNotNull(titleBar.inflateFilter()).apply {
            itemAnimator = itemAnimatorWithoutChangeAnimations()
        }

        mViewModel.selectSubscribe(MainState::listOfColors) {
            configureColorAdapter(colorRecycler, it, mViewModel)
        }

//        val tagRecycler = requireNotNull(titleBar.inflateFilter()).apply {
//            itemAnimator = itemAnimatorWithoutChangeAnimations()
//            updatePadding(
//                left = 24.toDp(resources),
//                right = 24.toDp(resources),
//                bottom = 8.toDp(resources)
//            )
//        }
//
//        mViewModel.selectSubscribe(MainState::listOfTags) {
//            configureTagAdapter(tagRecycler, it, mViewModel)
//        }

        val onFilterListener = {
            TransitionManager.beginDelayedTransition(titleBar.rootView as ViewGroup, transition)

            colorRecycler.isVisible = !colorRecycler.isVisible
//            tagRecycler.isVisible = !tagRecycler.isVisible && (tagRecycler.adapter?.itemCount != 0)

            val icon = if (!colorRecycler.isVisible) {
                R.drawable.ic_filter
            } else {
                R.drawable.ic_check
            }

            toolbar.menu.findItem(R.id.filter).icon =
                ContextCompat.getDrawable(requireContext(), icon)
        }

        toolbar.inflateMenu(R.menu.main)
        toolbar.updatePadding(left = 8.toDp(resources), right = 8.toDp(resources))
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.filter -> onFilterListener.invoke()
            }
            true
        }
    }

    override fun layoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context).apply {
        this.initialPrefetchItemCount = 5
    }

    private fun openItem(view: View, item: SiteAndLastSnap) = launch {
        val contentTypes = mViewModel.getRecentContentTypes(item.site.id)

        if (item.snap == null || contentTypes.firstOrNull()?.count?.let { it > 1 } != true) {
            MaterialDialog(view.context, BottomSheet(LayoutMode.WRAP_CONTENT))
                .customView(R.layout.epoxyrecyclerview)
                .show {
                    (this.getCustomView() as? EpoxyRecyclerView)?.withModels {
                        noChangesDetected {
                            id("no changes detected")
                            colorTint(item.site.colors.second)
                        }
                    }
                }
        } else {
            navigateTo(item.snap?.contentType, item)
        }
    }

    private fun navigateTo(selectedType: String?, item: SiteAndLastSnap) {
        val bundle = bundleOf(
            MainActivity.SITEID to item.site.id,
            MainActivity.TITLE to item.site.title,
            MainActivity.URL to item.site.url,
            MainActivity.TYPE to selectedType,
            MainActivity.LASTCHANGE to item.snap?.timestamp?.convertTimestampToDate()
        )

        val extras = view?.findViewWithTag<View>(item.site.id)?.let {
            FragmentNavigatorExtras(it to getString(R.string.shared_transition))
        }

        val destination = when {
            selectedType == "application/pdf" -> R.id.action_mainFragment_to_pdfFragment
            selectedType?.contains("image") == true -> R.id.action_mainFragment_to_imageCarouselFragment
            else -> R.id.action_mainFragment_to_textFragment
        }

        view?.findNavController()?.navigate(destination, bundle, null, extras)
    }
}
