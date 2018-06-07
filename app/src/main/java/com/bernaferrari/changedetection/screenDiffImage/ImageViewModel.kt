package com.bernaferrari.changedetection.screenDiffImage

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.bernaferrari.changedetection.data.MinimalSnap
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SnapsRepository

/**
 * Exposes the data to be used in the site diff screen.
 */
class ImageViewModel(
    context: Application,
    private val mSnapsRepository: SnapsRepository
) : AndroidViewModel(context) {

    /**
     * Called to remove a diff
     *
     * @param id The diff url to be removed.
     */
    fun removeSnap(id: String) {
        mSnapsRepository.deleteSnap(id)
    }


    fun getAllSnapsPagedForId(id: String): LiveData<PagedList<Snap>> {
        return LivePagedListBuilder(
            mSnapsRepository.getHeavySnapForPaging(id), PagedList.Config.Builder()
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(ENABLE_PLACEHOLDERS)
                .build()
        ).build()
    }

    fun getAllMinimalSnapsForId(id: String): LiveData<List<MinimalSnap>> {
        return mSnapsRepository.getMinimalSnaps(id)
    }

    companion object {
        /**
         * A good page size is a value that fills at least a screen worth of content on a large
         * device so the User is unlikely to see a null item.
         * You can play with this constant to observe the paging behavior.
         * <p>
         * It's possible to vary this with list device size, but often unnecessary, unless a user
         * scrolling on a large device is expected to scroll through items more quickly than a small
         * device, such as when the large device uses a grid layout of items.
         */
        private const val PAGE_SIZE = 3

        /**
         * If placeholders are enabled, PagedList will report the full size but some items might
         * be null in onBind method (PagedListAdapter triggers a rebind when data is loaded).
         * <p>
         * If placeholders are disabled, onBind will never receive null but as more pages are
         * loaded, the scrollbars will jitter as new pages are loaded. You should probably disable
         * scrollbars if you disable placeholders.
         */
        private const val ENABLE_PLACEHOLDERS = true
    }
}
