package com.filmatube.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
    }

    private fun initAppCheck() {
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.setTokenAutoRefreshEnabled(true)
        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        appCheck.installAppCheckProviderFactory(factory)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .build()
}
