package com.example.changedetection.data.source.local

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation
import com.example.changedetection.data.Diff
import com.example.changedetection.data.Site

class UserAndAllPets {
    @Embedded
    var site: Site? = null

    @Relation(
        parentColumn = "siteId",
        entityColumn = "siteId"
    )
    var diffs: List<Diff> = ArrayList()
}

data class SiteAndLastDiff(val site: Site, val diff: Diff?)