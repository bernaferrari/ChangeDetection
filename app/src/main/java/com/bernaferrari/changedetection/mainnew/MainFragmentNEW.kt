package com.bernaferrari.changedetection.mainnew

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.setPadding
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.afollestad.materialdialogs.MaterialDialog
import com.airbnb.mvrx.fragmentViewModel
import com.bernaferrari.changedetection.*
import com.bernaferrari.changedetection.core.simpleController
import com.bernaferrari.changedetection.extensions.*
import com.bernaferrari.changedetection.groupie.EmptyItem
import com.bernaferrari.changedetection.groupie.ItemContentType
import com.bernaferrari.changedetection.groupie.LoadingItem
import com.bernaferrari.changedetection.groupie.MainCardItem
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.util.GradientColors.getGradientDrawable
import com.bernaferrari.ui.dagger.DaggerBaseToolbarFragment
import com.orhanobut.logger.Logger
import com.tapadoo.alerter.Alerter
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.main_fragment.*
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
        (activity as AppCompatActivity?)?.setSupportActionBar(toolbar)

        mViewModel.sortAlphabetically = Injector.get().sharedPrefs().getBoolean("sortByName", false)

        recyclerView.apply {
            itemAnimator = itemAnimatorWithoutChangeAnimations()
            setPadding(16.toDp(resources))
        }

        mViewModel.outputStatus.observe(this, Observer { list ->
            mViewModel.live.accept(list.filter { it.state != WorkInfo.State.SUCCEEDED })
        })
//        mViewModel.loadSites().observe(this, Observer(::updateList))
//        mViewModel.getOutputStatus.observe(this, Observer(::workOutput))

        mViewModel.fetchData()

        viewContainer.inflateAddButton().setOnClickListener {
            it.findNavController().navigate(R.id.action_mainFragmentNEW_to_addNew)
        }
    }

    override fun layoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context).apply {
        this.initialPrefetchItemCount = 5
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return true
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

    private fun workOutput(it: List<WorkInfo>?) {
        val result = it ?: return

//        if (Injector.get().sharedPrefs().getBoolean("debug", false)) {
//            val sb = StringBuilder()
//            result.forEach { sb.append("${it.state.name}\n") }
//            if (sb.isNotEmpty()) {
//                sb.setLength(sb.length - 1) // Remove the last \n from the string
//            }
//            Logger.d("workerLog: $sb")
//            Snackbar.make(view!!, sb, Snackbar.LENGTH_SHORT).show()
//        }

        if (result.any { it.state == WorkInfo.State.SUCCEEDED }) {
            mViewModel.updateItems()
            Logger.d("Just refreshed")
        }
    }

    private val reloadCallback = { item: MainCardItem ->
        reload(item, true)
    }

    private fun reloadEach(item: MainCardItem?) {
        reload(item, false)
    }

    private fun reload(item: MainCardItem?, force: Boolean = false) {
        if (item !is MainCardItem || (!item.site.isSyncEnabled && !force)) return

        item.startSyncing()

        launch(Dispatchers.Main) {
            val (contentTypeCharset, content) = WorkerHelper.fetchFromServer(
                item.site.url
            )
            updateSiteAndSnap(contentTypeCharset, content, item)
        }
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

