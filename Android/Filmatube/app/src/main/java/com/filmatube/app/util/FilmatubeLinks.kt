package com.filmatube.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Public web destinations the app links out to.
 *
 * The legal pages live on the site rather than being duplicated in the app: a second copy
 * would drift from the one people actually agreed to, and the site version is what the
 * Terms themselves reference.
 */
object FilmatubeLinks {
    const val SITE = "https://filmatube--filmatubelive.europe-west4.hosted.app"
    const val TERMS = "$SITE/terms"
    const val PRIVACY = "$SITE/privacy"

    /**
     * Open [url] in the browser. Silently does nothing if the device has no handler —
     * unlikely on a phone, but an emulator image without a browser would otherwise crash.
     */
    fun open(context: Context, url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}
