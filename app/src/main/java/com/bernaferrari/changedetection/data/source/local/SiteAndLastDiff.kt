package com.bernaferrari.changedetection.data.source.local

import com.bernaferrari.changedetection.data.MinimalDiff
import com.bernaferrari.changedetection.data.Site

data class SiteAndLastDiff(val site: Site, val minimalDiff: MinimalDiff?)