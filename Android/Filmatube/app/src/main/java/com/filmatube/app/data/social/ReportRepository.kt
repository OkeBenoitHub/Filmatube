package com.filmatube.app.data.social

import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Content types that can be reported for moderation. */
enum class ReportTargetType(val value: String) {
    REVIEW("review"),
    COMMENT("comment"),
}

/**
 * Moderation reports at `reports/{id}` — users create, admins read/resolve
 * (surfaced in the admin report-moderation queue on Day 109).
 */
@Singleton
class ReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun report(
        type: ReportTargetType,
        movieId: String,
        targetId: String,
        reportedUserId: String,
        reason: String = "",
    ) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        firestore.collection("reports").add(
            mapOf(
                "type" to type.value,
                "movieId" to movieId,
                "targetId" to targetId,
                "reportedUserId" to reportedUserId,
                "reporterId" to uid,
                "reason" to reason,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }
}
