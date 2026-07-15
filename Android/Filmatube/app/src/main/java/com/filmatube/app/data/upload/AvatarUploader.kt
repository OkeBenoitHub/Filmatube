package com.filmatube.app.data.upload

import android.content.Context
import android.net.Uri
import com.filmatube.app.BuildConfig
import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads an image to Cloudflare R2 through the web presign API:
 *  1. POST /api/uploads/presign (Bearer = Firebase ID token) → presigned PUT URL + public URL
 *  2. PUT the bytes directly to R2
 *  3. return the public URL to store in Firestore
 */
@Singleton
class AvatarUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun uploadAvatar(uri: Uri): String = upload(uri, "avatar")

    /** Uploads a board cover to R2 (avatars bucket — user-writable) and returns its public URL. */
    suspend fun uploadBoardCover(uri: Uri): String = upload(uri, "board")

    private suspend fun upload(uri: Uri, name: String): String = withContext(ioDispatcher) {
        val user = auth.currentUser ?: error("Not signed in")
        val idToken = user.getIdToken(false).await().token ?: error("Missing ID token")

        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read image")
        val extension = contentType.substringAfterLast('/', "jpg").ifBlank { "jpg" }

        val presign = requestPresign(
            idToken = idToken,
            filename = "$name.$extension",
            contentType = contentType,
        )

        val putRequest = Request.Builder()
            .url(presign.uploadUrl)
            .put(bytes.toRequestBody(contentType.toMediaType()))
            .build()
        okHttpClient.newCall(putRequest).execute().use { response ->
            check(response.isSuccessful) { "Upload failed (${response.code})" }
        }

        presign.publicUrl ?: error("No public URL returned")
    }

    private fun requestPresign(idToken: String, filename: String, contentType: String): PresignResponse {
        val payload = json.encodeToString(
            PresignRequest(bucket = "avatars", filename = filename, contentType = contentType),
        )
        val request = Request.Builder()
            .url("${BuildConfig.WEB_API_BASE_URL}/api/uploads/presign")
            .addHeader("Authorization", "Bearer $idToken")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            check(response.isSuccessful) { "Presign failed (${response.code})" }
            json.decodeFromString<PresignResponse>(body)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
