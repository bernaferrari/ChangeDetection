package com.bernaferrari.changedetection.logs

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import com.airbnb.mvrx.fragmentViewModel
import com.bernaferrari.changedetection.*
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.mainnew.getLogsSubtitle
import com.bernaferrari.changedetection.mainnew.getTitle
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.ui.dagger.DaggerBaseRecyclerFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject


class LogsFragment : DaggerBaseRecyclerFragment() {

    private val viewModel: LogsRxViewModel by fragmentViewModel()
    @Inject
    lateinit var logsViewModelFactory: LogsRxViewModel.Factory

    private val pagingController = TestController()

    override fun epoxyController() = pagingController

    var siteMap = mapOf<String, Site>()
    var versionsCount: LiveData<Int>? = null
    var hasLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        versionsCount = viewModel.getVersionCount()

        versionsCount?.observe(this) {
            if (it == 0) {
                recyclerView.withModels {
                    logsEmptyItem {
                        id("empty")
                    }
                }
            } else {
                recyclerView.setController(pagingController)
            }
        }

        launch(Dispatchers.Main) {
            siteMap = viewModel.getSiteList()
            hasLoaded = true

            viewModel.pagedVersion()
                .observe(requireActivity()) {
                    pagingController.submitList(it)
                }
        }
    }

    inner class TestController : PagedListEpoxyController<Snap>() {
        override fun buildItemModel(currentPosition: Int, item: Snap?): EpoxyModel<*> {

            if (item == null || !siteMap.containsKey(item.siteId)) {
                // item should never be null since placeholders are disabled.
                return LogsItemBindingModel_().id("error")
            }

            val site = siteMap.getValue(item.siteId)

            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(item.timestamp),
                ZoneId.systemDefault()
            )

            return LogsItemBindingModel_()
                .id(item.snapId)
                .title(getTitle(site))
                .subtitle(getLogsSubtitle(item))
                .dateDayStr(dateTime.dayOfMonth.toString())
                .dateMonthStr(dateTime.format(DateTimeFormatter.ofPattern("MMM")))
                .size(item.contentSize.readableFileSize())
                .colorFirst(site.colors.first)
                .colorSecond(site.colors.second)
                .onClick { v ->
                    v.findNavController().navigate(
                        R.id.action_logsFragment_to_localBrowserFragment,
                        bundleOf(MainActivity.SNAPID to item.snapId, MainActivity.URL to site.url)
                    )
                }
        }

        init {
            isDebugLoggingEnabled = BuildConfig.DEBUG
        }

        override fun addModels(models: List<EpoxyModel<*>>) {

            when {
                models.isNotEmpty() -> marquee {
                    id("header")
                    title("Last Changes")
                    subtitle("${versionsCount?.value} changes detected")
                }
                versionsCount?.value == 0 && hasLoaded -> marquee { id("empty") }
                else -> loadingRow { id("loading") }
            }

            super.addModels(models)
        }

        override fun onExceptionSwallowed(exception: RuntimeException) {
            throw exception
        }
    }

    override fun onDestroyView() {
        recyclerView.adapter = null
        super.onDestroyView()
    }
}
