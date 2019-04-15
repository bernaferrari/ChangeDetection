package com.bernaferrari.changedetection.repo

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConstraintsRequired(
    val wifi: Boolean,
    val charging: Boolean,
    val batteryNotLow: Boolean,
    val deviceIdle: Boolean
) : Parcelable {
    constructor(list: List<String>) : this(
        list.getOrNull(0)?.toBoolean() ?: false,
        list.getOrNull(1)?.toBoolean() ?: false,
        list.getOrNull(2)?.toBoolean() ?: false,
        list.getOrNull(3)?.toBoolean() ?: false
    )
}