package com.filmatube.app.data.playback

import com.filmatube.app.BuildConfig
import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.FirebasePerformance
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a short-lived, token-protected playback URL for a movie's private R2 video.
 *
 * Calls the web API `GET /api/stream/{id}` with the Firebase ID token as a bearer;
 * the server presigns the `videos` bucket object (1 h expiry) so the URL can't be
 * shared or hotlinked. Mirrors [com.filmatube.app.data.upload.AvatarUploader]'s auth.
 */
@Singleton
class PlaybackRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun streamUrl(movieId: String): String = withContext(ioDispatcher) {
        val user = auth.currentUser ?: error("Not signed in")
        val idToken = user.getIdToken(false).await().token ?: error("Missing ID token")

        val request = Request.Builder()
            .url("${BuildConfig.WEB_API_BASE_URL}/api/stream/$movieId")
            .addHeader("Authorization", "Bearer $idToken")
            .get()
            .build()

        // Custom Performance trace: presign + network latency for playback start.
        val trace = FirebasePerformance.getInstance().newTrace("stream_url_resolve")
        trace.start()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                trace.putMetric("http_code", response.code.toLong())
                val body = response.body?.string().orEmpty()
                check(response.isSuccessful) { "Stream request failed (${response.code})" }
                json.decodeFromString<StreamResponse>(body).url
            }
        } finally {
            trace.stop()
        }
    }
}

@Serializable
private data class StreamResponse(val url: String, val expiresIn: Int = 3600)
