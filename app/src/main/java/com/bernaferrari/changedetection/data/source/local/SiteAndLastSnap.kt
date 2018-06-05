package com.bernaferrari.changedetection.data.source.local

import com.bernaferrari.changedetection.data.MinimalSnap
import com.bernaferrari.changedetection.data.Site

data class SiteAndLastSnap(val site: Site, val minimalSnap: MinimalSnap?)