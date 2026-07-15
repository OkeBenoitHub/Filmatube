package com.filmatube.app.data.download

/**
 * Pure cache-key logic (no Android deps) so it can be unit-tested directly.
 * A movie's playback URL is a presigned R2 URL whose query holds an expiring
 * signature; stripping the query yields a stable per-movie key so a download made
 * with one signed URL still resolves when the signature later changes.
 */
object DownloadKeys {
    fun stableCacheKey(url: String): String = url.substringBefore("?")
}
