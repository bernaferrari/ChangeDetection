/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.localization

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.util.localizedInteger
import com.bernaferrari.changedetection.util.localizedNumber
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

/** Keep Compose's locale selection and plural rules; format only numeric arguments with native locale rules. */
@Composable
internal fun stringResource(resource: StringResource, vararg args: Any): String = if (args.isEmpty()) {
    org.jetbrains.compose.resources.stringResource(resource)
} else {
    org.jetbrains.compose.resources.stringResource(resource, *args.localized())
}

@Composable
internal fun pluralStringResource(resource: PluralStringResource, quantity: Int, vararg args: Any): String = org.jetbrains.compose.resources.pluralStringResource(resource, quantity, *args.localized())

internal suspend fun getString(resource: StringResource, vararg args: Any): String = if (args.isEmpty()) {
    org.jetbrains.compose.resources.getString(resource)
} else {
    org.jetbrains.compose.resources.getString(resource, *args.localized())
}

internal suspend fun getPluralString(resource: PluralStringResource, quantity: Int, vararg args: Any): String = org.jetbrains.compose.resources.getPluralString(resource, quantity, *args.localized())

private fun Array<out Any>.localized(): Array<Any> = map { value ->
    when (value) {
        is Byte, is Short, is Int, is Long -> localizedInteger(value as Number)
        is Float, is Double -> localizedNumber((value as Number).toDouble(), maxFractionDigits = 2)
        else -> value
    }
}.toTypedArray()
