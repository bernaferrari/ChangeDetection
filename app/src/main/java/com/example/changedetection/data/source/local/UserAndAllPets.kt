package com.example.changedetection.data.source.local

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation
import com.example.changedetection.data.Diff
import com.example.changedetection.data.Task

class UserAndAllPets {
    @Embedded
    var task: Task? = null

    @Relation(parentColumn = "entryid",
    entityColumn = "owner")
    var diffs: List<Diff> = ArrayList()
}

class TestLast {
    @Relation(parentColumn = "entryid",
        entityColumn = "owner")
    var diffs: Diff? = null
}

data class SiteAndLastDiff (val task: Task, val diff: Diff)