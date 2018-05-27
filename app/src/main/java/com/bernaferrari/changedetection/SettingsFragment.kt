package com.bernaferrari.changedetection

import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import com.bernaferrari.changedetection.groupie.DialogItemInterval
import com.bernaferrari.changedetection.groupie.DialogItemSeparator
import com.bernaferrari.changedetection.groupie.DialogItemSwitch
import com.bernaferrari.changedetection.ui.RoundedBottomSheetDialogFragment
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.orhanobut.logger.Logger
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.settings.view.*

class SettingsFragment : RoundedBottomSheetDialogFragment() {
    val color: Int by lazy { ContextCompat.getColor(requireActivity(), R.color.FontStrong) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings, container, false)
        val groupAdapter = GroupAdapter<ViewHolder>()

        val updating = mutableListOf<Item>()
        val syncSettings = mutableListOf<Item>()
        val syncSection = Section()
        val sharedPrefs = Application.instance!!.sharedPrefs("workerPreferences")

        fun updateSharedPreferences(key: String, value: Boolean) {
            sharedPrefs.edit {
                putBoolean(key, value)
            }
            WorkerHelper.updateWorkerWithConstraints(sharedPrefs)
        }

        updating += DialogItemSwitch(
            "Background Sync",
            IconicsDrawable(context, GoogleMaterial.Icon.gmd_sync).color(color),
            sharedPrefs.getBoolean("backgroundSync", false)
        ) {
            sharedPrefs.edit {
                putBoolean("backgroundSync", it.isSwitchOn)
            }

            if (it.isSwitchOn) {
                syncSection.update(syncSettings)
                WorkerHelper.updateWorkerWithConstraints(sharedPrefs)
            } else {
                syncSection.update(mutableListOf())
                WorkerHelper.cancelWork()
            }
        }

        syncSettings += DialogItemInterval(
            "Sync Interval",
            sharedPrefs.getLong(WorkerHelper.DELAY, 60).toInt()
        ) {
            sharedPrefs.edit {
                putLong(WorkerHelper.DELAY, it)
            }
            WorkerHelper.updateWorkerWithConstraints(sharedPrefs)
            Logger.d("Reloaded! $it min")
        }

        syncSettings += DialogItemSeparator()

        syncSettings += DialogItemSwitch(
            "WiFi on",
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_wifi).color(
                color
            ),
            sharedPrefs.getBoolean(WorkerHelper.WIFI, false)
        ) {
            updateSharedPreferences(WorkerHelper.WIFI, it.isSwitchOn)
        }

        syncSettings += DialogItemSwitch(
            "Charging",
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_battery_charging).color(
                color
            ),
            sharedPrefs.getBoolean(WorkerHelper.CHARGING, false)
        ) {
            updateSharedPreferences(WorkerHelper.CHARGING, it.isSwitchOn)
        }

        syncSettings += DialogItemSwitch(
            "Battery not low",
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_battery_20).color(
                color
            ),
            sharedPrefs.getBoolean(WorkerHelper.BATTERYNOTLOW, false)
        ) {
            updateSharedPreferences(WorkerHelper.BATTERYNOTLOW, it.isSwitchOn)
        }

        if (Build.VERSION.SDK_INT >= 23) {
            syncSettings += DialogItemSwitch(
                "Device Idle",
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_sleep).color(
                    color
                ),
                sharedPrefs.getBoolean(WorkerHelper.IDLE, false)
            ) {
                updateSharedPreferences(WorkerHelper.IDLE, it.isSwitchOn)
            }
        }

        if (sharedPrefs.getBoolean("backgroundSync", false)) {
            syncSection.update(syncSettings)
        }

        view.defaultRecycler.run {
            layoutManager = LinearLayoutManager(this.context)

            adapter = groupAdapter.apply {
                if (this.itemCount == 0) {
                    this.add(Section(updating))
                    this.add(syncSection)
                }
            }
        }

        return view
    }
}

