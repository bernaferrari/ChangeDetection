package com.bernaferrari.changedetection.detailsvisual

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class VisualViewModel @Inject constructor(
    private val snapsRepository: SnapsRepository
) : ViewModel() {

    fun removeSnap(snapId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snapsRepository.deleteSnap(snapId)
        }
    }

    fun getSnapsPaged(siteId: String, contentType: String): Flow<PagingData<Snap>> {
        val filter = when {
            contentType == "application/pdf" -> "%pdf"
            contentType.startsWith("image/") -> "image%"
            else -> "%"
        }

        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = ENABLE_PLACEHOLDERS
            ),
            pagingSourceFactory = { snapsRepository.getSnapForPaging(siteId, filter) }
        ).flow.cachedIn(viewModelScope)
    }

    fun getSnapsFiltered(siteId: String, contentType: String): Flow<List<Snap>> = flow {
        val filter = when {
            contentType == "application/pdf" -> "%pdf"
            contentType.startsWith("image/") -> "image%"
            else -> "%"
        }

        val snaps = withContext(Dispatchers.IO) {
            snapsRepository.getAllSnapsFromSite(siteId).filter { snap ->
                when {
                    contentType == "application/pdf" -> snap.contentType?.contains("pdf") == true
                    contentType.startsWith("image/") -> snap.contentType?.startsWith("image/") == true
                    else -> true
                }
            }
        }
        emit(snaps)
    }

    fun shareSnap(snap: Snap, context: Context) {
        try {
            val file = File(context.filesDir, snap.snapId)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = snap.contentType ?: "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share snapshot"))
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun openInBrowser(url: String, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error
        }
    }

    companion object {
        private const val PAGE_SIZE = 10
        private const val ENABLE_PLACEHOLDERS = true

        fun formatTimestamp(timestamp: Long): String {
            val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }
    }
}
