package com.filmatube.app.data.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.filmatube.app.data.playback.PlaybackRepository
import com.filmatube.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues, observes and removes offline movie downloads via [FilmatubeDownloadService]. */
@UnstableApi
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackRepository: PlaybackRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val downloadManager: DownloadManager get() = DownloadUtil.getDownloadManager(context)

    /** Resolve the token-protected R2 URL and enqueue a download keyed by movie id. */
    suspend fun download(movieId: String, title: String) = withContext(ioDispatcher) {
        val url = playbackRepository.streamUrl(movieId)
        val request = DownloadRequest.Builder(movieId, Uri.parse(url))
            .setData(title.toByteArray())
            .build()
        DownloadService.sendAddDownload(context, FilmatubeDownloadService::class.java, request, false)
    }

    fun remove(movieId: String) {
        DownloadService.sendRemoveDownload(context, FilmatubeDownloadService::class.java, movieId, false)
    }

    /** All downloads, refreshed whenever any changes. */
    fun downloads(): Flow<List<Download>> = callbackFlow {
        fun snapshot(): List<Download> {
            val list = mutableListOf<Download>()
            downloadManager.downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) list.add(cursor.download)
            }
            return list
        }

        trySend(snapshot())
        val listener = object : DownloadManager.Listener {
            override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
                trySend(snapshot())
            }

            override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
                trySend(snapshot())
            }
        }
        downloadManager.addListener(listener)
        awaitClose { downloadManager.removeListener(listener) }
    }
}
