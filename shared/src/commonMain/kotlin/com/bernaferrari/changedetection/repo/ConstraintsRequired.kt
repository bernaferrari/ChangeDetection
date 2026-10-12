package com.bernaferrari.changedetection.repo

import kotlinx.serialization.Serializable

@Serializable
class ConstraintsRequired(
    val wifi: Boolean,
    val charging: Boolean,
    val batteryNotLow: Boolean,
    val deviceIdle: Boolean,
)
