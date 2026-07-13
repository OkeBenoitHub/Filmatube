package com.filmatube.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.filmatube.app.data.notifications.FilmatubeNotificationChannels
import dagger.hilt.android.HiltAndroidApp

/**
 * Filmatube application entry point and Hilt dependency-graph root.
 *
 * - Initializes Firebase **App Check** (Play Integrity in release, debug provider in debug).
 * - Disables Crashlytics collection in debug builds.
 * - Provides the global Coil [ImageLoader].
 *
 * (Firebase itself auto-initializes from `google-services.json` before `onCreate`.)
 */
@HiltAndroidApp
class FilmatubeApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initAppCheck()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        FilmatubeNotificationChannels.createAll(this)
    }

    private fun initAppCheck() {
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.setTokenAutoRefreshEnabled(true)
        // Provider factory is variant-specific: debug provider (src/debug) vs
        // Play Integrity (src/release). Keeps the debug provider out of release builds.
        appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            // Posters/backdrops are immutable per URL → cache aggressively across sessions.
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // up to 25% of app RAM for decoded bitmaps
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024) // 256 MB on-disk
                    .build()
            }
            .respectCacheHeaders(false) // R2/CDN images are content-addressed; ignore no-cache
            .build()
}
