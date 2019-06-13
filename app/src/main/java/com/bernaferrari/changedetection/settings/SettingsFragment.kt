package com.bernaferrari.changedetection.settings

import android.os.Bundle
import android.view.View
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.activityViewModel
import com.bernaferrari.base.mvrx.simpleController
import com.bernaferrari.changedetection.*
import com.bernaferrari.ui.extras.BaseRecyclerFragment

class SettingsFragment : BaseRecyclerFragment() {

    private val viewModel: SettingsViewModel by activityViewModel()

    override fun epoxyController(): EpoxyController = simpleController(viewModel) { state ->

        println("state is: ${state.data}")
        if (state.data is Loading) {
            loadingRow { id("loading") }
        }

        if (state.data.complete) {

            marquee {
                id("header")
                title("Settings")
                subtitle("Version ${BuildConfig.VERSION_NAME}")
            }

            val lightMode = state.data()?.lightMode ?: true

            SettingsSwitchBindingModel_()
                .id("light mode")
                .title("Light mode")
                .switchIsVisible(true)
                .switchIsOn(lightMode)
                .clickListener { v ->
                    Injector.get().isLightTheme().set(!lightMode)
                    activity?.recreate()
                }
                .addTo(this)

            val colorBySdk = state.data()?.colorBySdk ?: true

            val showSystemApps = state.data()?.showSystemApps ?: true

            SettingsSwitchBindingModel_()
                .id("system apps")
                .title("Show system apps")
                .switchIsVisible(true)
                .switchIsOn(showSystemApps)
                .subtitle("Show all installed apps. This might increase loading time.")
                .clickListener { v ->
                    Injector.get().showSystemApps().set(!showSystemApps)
                }
                .addTo(this)

            SettingsSwitchBindingModel_()
                .id("color mode")
                .title("Color by targetSDK")
                .subtitle(if (colorBySdk) "Color will range from green (recent sdk) to red (old)." else "Color will match the icon's palette.")
                .switchIsVisible(true)
                .switchIsOn(colorBySdk)
                .clickListener { v ->
                    Injector.get().isColorBySdk().set(!colorBySdk)
                }
                .addTo(this)

            val orderBySdk = state.data()?.orderBySdk ?: true

            SettingsSwitchBindingModel_()
                .id("order by")
                .title("Order by targetSDK")
                .subtitle("Change the order of items")
                .switchIsVisible(true)
                .switchIsOn(orderBySdk)
                .clickListener { v ->
                    Injector.get().orderBySdk().set(!orderBySdk)
                }
                .addTo(this)

            val backgroundSync = state.data()?.backgroundSync ?: false

            SettingsSwitchBindingModel_()
                .id("background sync")
                .title("Background Sync")
                .icon(R.drawable.ic_sync)
                .subtitle(if (backgroundSync) "Enabled" else "Disabled")
                .clickListener { v ->
                    DialogBackgroundSync.show(requireActivity())
                }
                .addTo(this)

            SettingsSwitchBindingModel_()
                .id("about")
                .title("About")
                .icon(R.drawable.ic_info)
                .clickListener { v ->

                }
                .addTo(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val decoration = InsetDecoration(1, 0, 0x40FFFFFF)
//        recycler.addItemDecoration(decoration)
    }
}
