package com.filmatube.app.data.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import java.io.File
import java.util.concurrent.Executors

/**
 * Process-wide singletons for offline downloads. The [FilmatubeDownloadService] is created by
 * the system (not Hilt), so the shared [Cache]/[DownloadManager] live here — Media3's
 * recommended pattern. Only one [SimpleCache] may exist per directory.
 */
@UnstableApi
object DownloadUtil {
    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "filmatube_downloads"
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    private var databaseProvider: DatabaseProvider? = null
    private var downloadCache: Cache? = null
    private var downloadManager: DownloadManager? = null
    private var notificationHelper: DownloadNotificationHelper? = null

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

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        return downloadManager ?: run {
            val app = context.applicationContext
            DownloadManager(
                app,
                getDatabaseProvider(app),
                getDownloadCache(app),
                DefaultHttpDataSource.Factory(),
                Executors.newFixedThreadPool(3),
            ).also { downloadManager = it }
        }
    }

    @Synchronized
    fun getDownloadNotificationHelper(context: Context): DownloadNotificationHelper =
        notificationHelper ?: DownloadNotificationHelper(
            context.applicationContext,
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        ).also { notificationHelper = it }

    /** Read-through cache source so the player can play a downloaded movie offline. */
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val app = context.applicationContext
        val upstream = DefaultDataSource.Factory(app, DefaultHttpDataSource.Factory())
        return CacheDataSource.Factory()
            .setCache(getDownloadCache(app))
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory(null) // read-only during playback
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
