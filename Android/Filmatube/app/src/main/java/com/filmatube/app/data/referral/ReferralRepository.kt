package com.filmatube.app.data.referral

import com.filmatube.app.BuildConfig
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Referrals on Android (v1.3, Day 198).
 *
 * `referrals/{}` is written server-side only (the rules block client writes), so attribution goes
 * through the web endpoint `/api/referral` with the Firebase id-token as a bearer — the same auth
 * the upload presign uses. Reads (who I've referred) come straight from Firestore, which the rules
 * allow for the referrer.
 */
@Singleton
class ReferralRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val okHttpClient: OkHttpClient,
    private val prefs: UserPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /** The invite link this user shares (code = their uid). */
    fun inviteLink(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return "${BuildConfig.WEB_API_BASE_URL.trimEnd('/')}/invite/$uid"
    }

    /**
     * If an invite code was captured from a deep link, attribute it to the just-created account,
     * then clear it. Best-effort: the server ignores self-referral / repeat / unknown referrers,
     * so a failed or duplicate call is harmless.
     */
    suspend fun attributePendingInvite() = withContext(ioDispatcher) {
        val code = runCatching { prefs.pendingInviteCode.first() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return@withContext
        val idToken = runCatching { auth.currentUser?.getIdToken(false)?.await()?.token }.getOrNull()
            ?: return@withContext

        val payload = JSONObject().put("code", code).toString()
        val request = Request.Builder()
            .url("${BuildConfig.WEB_API_BASE_URL.trimEnd('/')}/api/referral")
            .addHeader("Authorization", "Bearer $idToken")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val ok = runCatching {
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
        // Clear whether or not it recorded — the code is single-use and the server is idempotent.
        if (ok) runCatching { prefs.clearPendingInviteCode() }
    }

    /** Ids of users this account has referred (realtime). Rules allow the referrer to read these. */
    fun observeMyReferralIds(): Flow<List<String>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = firestore.collection("referrals")
            .whereEqualTo("referrerId", uid)
            .addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.map { it.getString("referredId") ?: it.id } ?: emptyList()
                trySend(ids)
            }
        awaitClose { registration.remove() }
    }
}
