/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

import kotlinx.serialization.Serializable

@Serializable
class ConstraintsRequired(
    val wifi: Boolean,
    val charging: Boolean,
    val batteryNotLow: Boolean,
    val deviceIdle: Boolean,
) {
    constructor(list: List<String>) : this(
        list.getOrNull(0)?.toBoolean() ?: false,
        list.getOrNull(1)?.toBoolean() ?: false,
        list.getOrNull(2)?.toBoolean() ?: false,
        list.getOrNull(3)?.toBoolean() ?: false,
    )
}
