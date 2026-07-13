package com.filmatube.app.data.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over Crashlytics for recording **non-fatal** errors that would otherwise be
 * swallowed by `runCatching`, with a bit of breadcrumb context. Collection is disabled in
 * debug builds (see [com.filmatube.app.FilmatubeApp]).
 */
@Singleton
class CrashReporter @Inject constructor() {
    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    fun log(message: String) = crashlytics.log(message)

    /** Record a handled exception with an optional short context breadcrumb. */
    fun recordNonFatal(throwable: Throwable, context: String? = null) {
        if (context != null) crashlytics.log(context)
        crashlytics.recordException(throwable)
    }

    fun setKey(key: String, value: String) = crashlytics.setCustomKey(key, value)
}
