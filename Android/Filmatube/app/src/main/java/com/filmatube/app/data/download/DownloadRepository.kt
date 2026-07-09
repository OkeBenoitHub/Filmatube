package com.filmatube.app.data.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import com.filmatube.app.data.playback.PlaybackRepository
import com.filmatube.app.di.IoDispatcher
import com.filmatube.app.domain.model.Movie
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SubtitleDto(val lang: String, val url: String)

/** One row in the download queue / library. */
@OptIn(UnstableApi::class)
data class DownloadItem(
    val movieId: String,
    val title: String,
    val posterUrl: String,
    val state: Int,
    val percent: Float,
    val bytesDownloaded: Long,
) {
    val isComplete: Boolean get() = state == Download.STATE_COMPLETED
    val isPaused: Boolean get() = state == Download.STATE_STOPPED
}

/** Enqueues, observes, controls and persists offline movie downloads. */
@UnstableApi
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val dao: DownloadedMovieDao,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val downloadManager: DownloadManager get() = DownloadUtil.getDownloadManager(context)

    /** Resolve the token-protected R2 URL, persist display metadata, and enqueue the download. */
    suspend fun download(movie: Movie) = withContext(ioDispatcher) {
        val url = playbackRepository.streamUrl(movie.id)
        dao.upsert(
            DownloadedMovie(
                movieId = movie.id,
                titleEn = movie.title.en,
                titleFr = movie.title.fr,
                posterUrl = movie.posterUrl,
                backdropUrl = movie.backdropUrl,
                durationMin = movie.duration,
                subtitlesJson = json.encodeToString(movie.subtitleTracks.map { SubtitleDto(it.lang, it.url) }),
                addedAt = System.currentTimeMillis(),
            ),
        )
        val request = DownloadRequest.Builder(movie.id, Uri.parse(url))
            .setData(movie.title.en.toByteArray())
            .build()
        DownloadService.sendAddDownload(context, FilmatubeDownloadService::class.java, request, false)
    }

    fun pause(movieId: String) =
        DownloadService.sendSetStopReason(context, FilmatubeDownloadService::class.java, movieId, STOP_REASON_PAUSED, false)

    fun resume(movieId: String) =
        DownloadService.sendSetStopReason(context, FilmatubeDownloadService::class.java, movieId, Download.STOP_REASON_NONE, false)

    suspend fun cancel(movieId: String) = withContext(ioDispatcher) {
        DownloadService.sendRemoveDownload(context, FilmatubeDownloadService::class.java, movieId, false)
        dao.delete(movieId)
    }

    /** Wi-Fi-only vs any-network for downloads. */
    fun setWifiOnly(wifiOnly: Boolean) {
        downloadManager.requirements =
            Requirements(if (wifiOnly) Requirements.NETWORK_UNMETERED else Requirements.NETWORK)
    }

    /** Cached subtitle tracks for a downloaded movie (for offline playback). */
    suspend fun subtitlesFor(movieId: String): List<SubtitleDto> = withContext(ioDispatcher) {
        val meta = dao.get(movieId) ?: return@withContext emptyList()
        runCatching { json.decodeFromString<List<SubtitleDto>>(meta.subtitlesJson) }.getOrDefault(emptyList())
    }

    suspend fun isDownloaded(movieId: String): Boolean = withContext(ioDispatcher) {
        downloadManager.downloadIndex.getDownload(movieId)?.state == Download.STATE_COMPLETED
    }

    val downloadedMovies: Flow<List<DownloadedMovie>> = dao.observeAll()

    /** The live queue: Media3 download state joined with persisted display metadata. */
    fun items(): Flow<List<DownloadItem>> = combine(rawDownloads(), dao.observeAll()) { downloads, metas ->
        downloads.map { d ->
            val meta = metas.firstOrNull { it.movieId == d.request.id }
            DownloadItem(
                movieId = d.request.id,
                title = meta?.titleEn?.ifBlank { null } ?: String(d.request.data),
                posterUrl = meta?.posterUrl ?: "",
                state = d.state,
                percent = d.percentDownloaded,
                bytesDownloaded = d.bytesDownloaded,
            )
        }
    }

    private fun rawDownloads(): Flow<List<Download>> = callbackFlow {
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

    private companion object {
        const val STOP_REASON_PAUSED = 1
    }
}
