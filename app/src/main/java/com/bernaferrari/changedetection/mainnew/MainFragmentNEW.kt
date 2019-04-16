package com.bernaferrari.changedetection.mainnew

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.work.WorkInfo
import com.afollestad.materialdialogs.MaterialDialog
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.fragmentViewModel
import com.bernaferrari.base.misc.toDp
import com.bernaferrari.changedetection.*
import com.bernaferrari.changedetection.addedit.ColorPickerItemEpoxy_
import com.bernaferrari.changedetection.core.simpleController
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.groupie.EmptyItem
import com.bernaferrari.changedetection.groupie.ItemContentType
import com.bernaferrari.changedetection.groupie.LoadingItem
import com.bernaferrari.changedetection.groupie.MainCardItem
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.util.GradientColors
import com.bernaferrari.changedetection.util.GradientColors.getGradientDrawable
import com.bernaferrari.ui.dagger.DaggerBaseToolbarFragment
import com.orhanobut.logger.Logger
import com.tapadoo.alerter.Alerter
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.addnewfragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainFragmentNEW : DaggerBaseToolbarFragment() {

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
                title("Empty")
            }
        }

        state.listOfItems.forEach {

            MainCardItem_()
                .id(it.site.id)
                .site(it.site)
                .lastSnap(it.snap)
                .title(getTitle(it.site))
                .lastSyncStr(getLastSync(requireContext(), it, it.isSyncing))
                .lastDiffStr(getLastDiff(requireContext(), it.snap))
                .syncingNow(it.isSyncing)
                .onReload { v ->
                    WorkerHelper.reloadSite(it.site)
                }
                .onLongClick { v ->
                    val bundle = bundleOf("site" to it.site)
                    v.findNavController().navigate(R.id.action_mainFragmentNEW_to_addNew, bundle)
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

        mViewModel.outputStatus.observe(this, Observer { list ->
            mViewModel.workManagerObserver.accept(list.filter { it.state != WorkInfo.State.SUCCEEDED })
        })

        viewContainer.inflateAddButton().setOnClickListener {
            it.findNavController().navigate(R.id.action_mainFragmentNEW_to_addNew)
        }

        val colorRecycler = requireNotNull(titleBar.inflateFilter())
        colorRecycler.apply { itemAnimator = itemAnimatorWithoutChangeAnimations() }

        val tagRecycler = requireNotNull(titleBar.inflateFilter())

        mViewModel.selectSubscribe(MainState::listOfColors) {
            configureColorAdapter(colorRecycler, it)
        }

        toolbar.inflateMenu(R.menu.main)
        toolbar.updatePadding(left = 8.toDp(resources), right = 8.toDp(resources))
        toolbar.setOnMenuItemClickListener {

            TransitionManager.beginDelayedTransition(titleBar.rootView as ViewGroup, transition)

            when (it.itemId) {
                R.id.colorFilter -> {
                    colorRecycler.isVisible = !colorRecycler.isVisible

                    val icon = if (!colorRecycler.isVisible) {
                        R.drawable.ic_filter
                    } else {
                        R.drawable.ic_check
                    }

                    toolbar.menu.findItem(R.id.colorFilter).icon =
                        ContextCompat.getDrawable(requireContext(), icon)
                }
                R.id.tagFilter -> {


                }
            }
            true
        }
    }


    private fun configureColorAdapter(
        colorSelector: EpoxyRecyclerView,
        colorsList: List<ColorGroup>
    ) {

        val listOfSelectedItems = (mViewModel.selectedColors.value ?: emptyList()).toMutableList()

        val controller = simpleController {

            // Create each color picker item, checking for the first (because it needs extra margin)
            // and checking for the one which is selected (so it becomes selected)
            colorsList.forEachIndexed { index, it ->

                ColorPickerItemEpoxy_()
                    .id("picker $index")
                    .allowDeselection(true)
                    .switchIsOn(it in listOfSelectedItems)
                    .gradientColor(it)
                    .onClick { v ->

                        val elementIndex = listOfSelectedItems.indexOf(v)
                        if (elementIndex != -1) {
                            listOfSelectedItems.removeAt(elementIndex)
                        } else {
                            listOfSelectedItems += v
                        }

                        mViewModel.selectedColors.accept(listOfSelectedItems)

                        this.requestModelBuild()
                    }
                    .addTo(this)
            }
        }

        colorSelector.setController(controller)
        controller.requestModelBuild()
    }


    private fun configureTagsChooserRecycler(tagSelector: EpoxyRecyclerView?) {

        val colorsList = GradientColors.gradients

        val listOfSelectedItems = mutableListOf<ColorGroup>()

        val controller = simpleController {

            // Create each color picker item, checking for the first (because it needs extra margin)
            // and checking for the one which is selected (so it becomes selected)
            colorsList.forEachIndexed { index, it ->

                ColorPickerItemEpoxy_()
                    .id("picker $index")
                    .allowDeselection(true)
                    .switchIsOn(it in listOfSelectedItems)
                    .gradientColor(it)
                    .onClick { v ->

                        val elementIndex = listOfSelectedItems.indexOf(v)
                        if (elementIndex != -1) {
                            listOfSelectedItems.removeAt(elementIndex)
                        } else {
                            listOfSelectedItems += v
                        }

                        this.requestModelBuild()
                    }
                    .addTo(this)
            }
        }

        tagSelector?.apply {
            this.itemAnimator = itemAnimatorWithoutChangeAnimations()
            this.overScrollMode = View.OVER_SCROLL_NEVER
            this.setController(controller)
            this.requestModelBuild()
        }
    }

    override fun layoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context).apply {
        this.initialPrefetchItemCount = 5
    }

    private fun openItem(item: MainCardItem) {
        val customView = parentLayout.inflate(R.layout.recyclerview)

        val bottomSheet = customView.createBottomSheet()

        val bottomSheetAdapter = GroupAdapter<ViewHolder>().apply {
            add(LoadingItem())
        }

        customView.findViewById<RecyclerView>(R.id.recycler)
            .adapter = bottomSheetAdapter

        launch {
            updateBottomSheet(item, bottomSheetAdapter, bottomSheet)
        }
    }

    private suspend fun updateBottomSheet(
        item: MainCardItem,
        bottomSheetAdapter: GroupAdapter<ViewHolder>,
        bottomSheet: com.google.android.material.bottomsheet.BottomSheetDialog
    ): Unit = withContext(Dispatchers.Main) {

        val contentTypes = mViewModel.getRecentContentTypes(item.site.id)
        val selectedType = item.lastSnap?.contentType

        bottomSheetAdapter.clear()

        when {
            contentTypes.size <= 1 && contentTypes.firstOrNull()?.count?.let { it > 1 } != true -> {
                bottomSheet.show()
                bottomSheetAdapter.add(EmptyItem(item.site.colors.second))
            }
            contentTypes.size == 1 -> {
                navigateTo(selectedType, item)
            }
            else -> {
                bottomSheet.show()

                val remove: ((String) -> (Unit)) = {
                    MaterialDialog(requireContext())
                        .title(R.string.remove)
                        .message(R.string.remove_content)
                        .positiveButton(R.string.yes) { _ ->
                            GlobalScope.launch {
                                mViewModel.removeSnapsByType(item.site.id, it)
                                updateBottomSheet(item, bottomSheetAdapter, bottomSheet)
                            }
                        }
                        .negativeButton(R.string.no)
                        .show()
                }

                contentTypes.forEach {
                    bottomSheetAdapter.add(
                        ItemContentType(
                            it.contentType,
                            it.count,
                            remove
                        ) { selected ->
                            bottomSheet.dismiss()
                            if (it.count > 1) {
                                navigateTo(selected, item)
                            } else {
                                view?.also { v ->
                                    com.google.android.material.snackbar.Snackbar.make(
                                        v,
                                        R.string.less_than_two,
                                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                                    )
                                        .show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun navigateTo(selectedType: String?, item: MainCardItem) {
        val bundle = bundleOf(
            MainActivity.SITEID to item.site.id,
            MainActivity.TITLE to item.site.title,
            MainActivity.URL to item.site.url,
            MainActivity.TYPE to selectedType,
            MainActivity.LASTCHANGE to item.lastSnap?.timestamp?.convertTimestampToDate()
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

    private fun updateSiteAndSnap(
        contentTypeCharset: String,
        content: ByteArray,
        item: MainCardItem
    ) {
        Logger.d("count size -> ${content.size}")

        val newSite = item.site.copy(
            timestamp = System.currentTimeMillis(),
            isSuccessful = content.isNotEmpty()
        )
        mViewModel.updateSite(newSite)

        // text/html;charset=UTF-8 needs to become text/html and UTF-8
        val snap = Snap(
            siteId = item.site.id,
            timestamp = newSite.timestamp,
            contentType = contentTypeCharset.split(";").first(),
            contentCharset = contentTypeCharset.findCharset(),
            contentSize = content.size
        )

        mViewModel.saveWebsite(snap, content).observe(this, Observer { isSuccess ->
            item.update(newSite)

            if (isSuccess != true) {
                return@Observer
            }

            Logger.d("snapId: " + snap.snapId)

            // Only show this toast when there was a change, which means, not on the first sync.
            if (item.lastSnap != null && activity != null) {
                Alerter.create(requireActivity())
                    .setTitle(
                        getString(
                            R.string.was_updated,
                            newSite.title?.takeIf { it.isNotBlank() } ?: newSite.url
                        )
                    )
                    .setBackgroundDrawable(
                        getGradientDrawable(newSite.colors)
                    )
                    .setIcon(R.drawable.ic_notification)
                    .show()
                    .also { alert ->
                        alert?.setOnClickListener {
                            openItem(item)
                            Alerter.hide()
                        }
                    }
            }

            item.update(snap)
//            sortList()
        })
    }
}

