package com.bernaferrari.changedetection.detailsvisual

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SnapsRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Exposes the data to be used in the site diff screen.
 */
class VisualViewModel(
    context: Application,
    private val mSnapsRepository: SnapsRepository
) : AndroidViewModel(context) {

    /**
     * Called to remove a diff
     *
     * @param id The diff url to be removed.
     */
    fun removeSnap(id: String) = GlobalScope.launch {
        mSnapsRepository.deleteSnap(id)
    }

    suspend fun getAllSnapsPagedForId(id: String, filter: String): LiveData<PagedList<Snap>> {
        return LivePagedListBuilder(
            mSnapsRepository.getSnapForPaging(id, filter), PagedList.Config.Builder()
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(ENABLE_PLACEHOLDERS)
                .build()
        ).build()
    }

    suspend fun getSnapsFiltered(id: String, filter: String): LiveData<List<Snap>> =
        mSnapsRepository.getSnapsFiltered(id, filter)

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
