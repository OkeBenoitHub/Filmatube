package com.filmatube.app.data.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.AesCipherDataSink
import androidx.media3.datasource.AesCipherDataSource
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import java.io.File
import java.util.concurrent.Executors

/**
 * Process-wide singletons for offline downloads (Media3's recommended pattern — the
 * [FilmatubeDownloadService] is created by the system, not Hilt). The cache is keyed by
 * the object path *without* the query string, so a movie's presigned R2 URL resolves to
 * the same cached content even after the signature expires.
 */
@UnstableApi
object DownloadUtil {
    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "filmatube_downloads"
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    private var databaseProvider: DatabaseProvider? = null
    private var downloadCache: Cache? = null
    private var downloadManager: DownloadManager? = null
    private var notificationHelper: DownloadNotificationHelper? = null

    /** Stable per-movie cache key: the URL path, ignoring the expiring signature query. */
    private val cacheKeyFactory = CacheKeyFactory { dataSpec ->
        dataSpec.key ?: DownloadKeys.stableCacheKey(dataSpec.uri.toString())
    }

    @Synchronized
    fun getDatabaseProvider(context: Context): DatabaseProvider =
        databaseProvider ?: StandaloneDatabaseProvider(context.applicationContext)
            .also { databaseProvider = it }

    @Synchronized
    fun getDownloadCache(context: Context): Cache {
        return downloadCache ?: run {
            val app = context.applicationContext
            val dir = File(app.getExternalFilesDir(null) ?: app.filesDir, DOWNLOAD_CONTENT_DIRECTORY)
            SimpleCache(dir, NoOpCacheEvictor(), getDatabaseProvider(app)).also { downloadCache = it }
        }
    }

    private fun cacheDataSourceFactory(context: Context, writable: Boolean): CacheDataSource.Factory {
        val app = context.applicationContext
        val cache = getDownloadCache(app)
        val key = DownloadEncryption.contentKey(app)
        val upstream = DefaultDataSource.Factory(app, DefaultHttpDataSource.Factory())
        // Cached bytes are AES-encrypted at rest: encrypt on write, decrypt on read.
        val readFactory = DataSource.Factory { AesCipherDataSource(key, FileDataSource()) }
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheKeyFactory(cacheKeyFactory)
            .setCacheReadDataSourceFactory(readFactory)
            .apply {
                if (writable) {
                    setCacheWriteDataSinkFactory(
                        DataSink.Factory {
                            AesCipherDataSink(key, CacheDataSink.Factory().setCache(cache).createDataSink())
                        },
                    )
                } else {
                    setCacheWriteDataSinkFactory(null)
                    setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                }
            }
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        return downloadManager ?: run {
            val app = context.applicationContext
            DownloadManager(
                app,
                DefaultDownloadIndex(getDatabaseProvider(app)),
                DefaultDownloaderFactory(cacheDataSourceFactory(app, writable = true), Executors.newFixedThreadPool(3)),
            ).also { downloadManager = it }
        }
    }

    @Synchronized
    fun getDownloadNotificationHelper(context: Context): DownloadNotificationHelper =
        notificationHelper ?: DownloadNotificationHelper(
            context.applicationContext,
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        ).also { notificationHelper = it }

    /** Read-only cache source so the player streams online *and* plays downloads offline. */
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory =
        cacheDataSourceFactory(context, writable = false)
}
