/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

import kotlinx.serialization.Serializable

/** A saved page's icon outline; automatic keeps the page's generated identity. */
@Serializable
enum class PageIconShape { AUTOMATIC, CIRCLE, ROUNDED, FLOWER, CLOVER, SUN, COOKIE, GEM, PUFFY }
