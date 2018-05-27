package com.bernaferrari.changedetection.data.source.local

import com.bernaferrari.changedetection.data.DiffWithoutValue
import com.bernaferrari.changedetection.data.Site

data class SiteAndLastDiff(val site: Site, val diff: DiffWithoutValue?)