package com.filmatube.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Filmatube application entry point and Hilt dependency graph root.
 *
 * Firebase / Crashlytics / App Check initialization is wired in on Day 5.
 */
@HiltAndroidApp
class FilmatubeApp : Application()
