package com.bernaferrari.changedetection.mainnew

import android.app.Dialog
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.airbnb.epoxy.EpoxyRecyclerView
import com.bernaferrari.base.misc.openInBrowser
import com.bernaferrari.changedetection.DialogItemSimpleBindingModel_
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.settings.InsetDecoration
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import dagger.android.support.DaggerDialogFragment
import kotlinx.android.synthetic.main.recyclerview.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class LongPressOptionsDialog : DaggerDialogFragment() {

    var color: Int = 0
    lateinit var site: Site

    @Inject
    lateinit var sitesDao: SitesDao

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity ?: throw IllegalStateException("Oh no!")
        site = arguments?.getParcelable("site") ?: throw IllegalStateException("No site!")

        return MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT))
            .title(text = site.title.takeIf { !it.isNullOrBlank() } ?: site.url)
            .customView(R.layout.epoxyrecyclerview)
            .show { getCustomView().recycler.setUpView() }
    }

    private fun CommunityMaterial.Icon.getIcon(): IconicsDrawable? {
        return IconicsDrawable(context, this).color(color)
    }

    private fun EpoxyRecyclerView.setUpView() {

        addItemDecoration(
            InsetDecoration(
                resources.getDimensionPixelSize(R.dimen.divider_height),
                resources.getDimensionPixelSize(R.dimen.long_press_separator_margin),
                ContextCompat.getColor(requireContext(), R.color.separator_color)
            )
        )

        color = site.colors.second

        withModels {

            DialogItemSimpleBindingModel_()
                .id("edit")
                .title(getString(R.string.edit))
                .drawable(CommunityMaterial.Icon.cmd_pencil.getIcon())
                .clickListener { v ->
                    NavHostFragment.findNavController(this@LongPressOptionsDialog)
                        .navigate(
                            R.id.action_mainLongPressSheet_to_addNew,
                            bundleOf("site" to site)
                        )
                }
                .addTo(this)

            DialogItemSimpleBindingModel_()
                .id("browser")
                .title(getString(R.string.open_in_browser))
                .drawable(CommunityMaterial.Icon.cmd_google_chrome.getIcon())
                .clickListener { v ->
                    requireContext().openInBrowser(site.url)
                }
                .addTo(this)

            // if item is disabled, makes no sense to enable/disable the notifications
            if (site.isNotificationEnabled) {

                val notificationTitle = site.takeIf { it.isNotificationEnabled }
                    ?.let { R.string.notification_disable }
                        ?: R.string.notification_enable

                val notificationIcon =
                    site.takeIf { it.isNotificationEnabled }
                        ?.let { CommunityMaterial.Icon.cmd_bell_off }
                            ?: CommunityMaterial.Icon.cmd_bell

                DialogItemSimpleBindingModel_()
                    .id("notification")
                    .title(context.getString(notificationTitle))
                    .drawable(notificationIcon.getIcon())
                    .clickListener { v ->
                        GlobalScope.launch(Dispatchers.IO) {
                            sitesDao.updateSite(site.copy(isSyncEnabled = !site.isNotificationEnabled))
                            navDismiss()
                        }
                    }
            }

            val syncTitle = site.takeIf { it.isSyncEnabled }
                ?.let { R.string.sync_disable } ?: R.string.sync_enable

            val syncIcon =
                site.takeIf { it.isSyncEnabled }
                    ?.let { CommunityMaterial.Icon.cmd_sync_off }
                        ?: CommunityMaterial.Icon.cmd_sync

            DialogItemSimpleBindingModel_()
                .id("sync")
                .title(context.getString(syncTitle))
                .drawable(syncIcon.getIcon())
                .clickListener { v ->
                    GlobalScope.launch(Dispatchers.IO) {
                        sitesDao.updateSite(site.copy(isSyncEnabled = !site.isSyncEnabled))
                        navDismiss()
                    }
                }
                .addTo(this)

            DialogItemSimpleBindingModel_()
                .id("remove")
                .title(context.getString(R.string.remove))
                .drawable(CommunityMaterial.Icon.cmd_delete.getIcon())
                .clickListener { v ->
                    GlobalScope.launch(Dispatchers.IO) {
                        sitesDao.deleteSiteById(site.id)
                        navDismiss()
                    }
                }
                .addTo(this)
        }
    }

    // using standard dismiss causes a crash if you re-open the fragment
    private fun navDismiss() {
        NavHostFragment.findNavController(this).popBackStack()
    }
}