package com.bernaferrari.changedetection.detailsText

import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextBottomDataSource @Inject constructor(
    private val snapsDao: SnapsDao
) : DictDataSource {

    override fun getItems(id: String, filter: String): Observable<List<Snap>> {
        return snapsDao.getSnapsWithChange(id, filter)
    }
}

interface DictDataSource {

    fun getItems(id: String, filter: String = "%"): Observable<List<Snap>>

}
