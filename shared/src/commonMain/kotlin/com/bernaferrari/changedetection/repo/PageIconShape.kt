package com.bernaferrari.changedetection.repo

import kotlinx.serialization.Serializable

/** A saved page's icon outline; automatic keeps the page's generated identity. */
@Serializable
enum class PageIconShape { AUTOMATIC, CIRCLE, ROUNDED, FLOWER, CLOVER, SUN, COOKIE, GEM, PUFFY }
