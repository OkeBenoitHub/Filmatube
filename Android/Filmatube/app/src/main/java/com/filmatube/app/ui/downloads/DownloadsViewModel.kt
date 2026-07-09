package com.filmatube.app.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.filmatube.app.data.download.DownloadItem
import com.filmatube.app.data.download.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val items = downloadRepository.items()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<DownloadItem>())

    /** Total bytes on disk, recomputed whenever the queue changes. */
    val storageUsed = items
        .map { downloadRepository.storageUsedBytes() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun pause(movieId: String) = downloadRepository.pause(movieId)
    fun resume(movieId: String) = downloadRepository.resume(movieId)
    fun cancel(movieId: String) {
        viewModelScope.launch { downloadRepository.cancel(movieId) }
    }
}
