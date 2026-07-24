package com.filmatube.app.data.achievements

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A user's rolled-up stats (`stats/{uid}`), built nightly by the `buildStats` function. */
data class UserStats(
    val totalWatchMinutes: Int = 0,
    val moviesCompleted: Int = 0,
    val reviewsWritten: Int = 0,
    val topGenres: List<String> = emptyList(),
    // Light gamification (Day 202).
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeklyCompleted: Int = 0,
    val weeklyGoal: Int = 3,
) {
    /** Whole hours watched — the headline figure. */
    val watchHours: Int get() = totalWatchMinutes / 60

    /** Progress toward this week's goal, clamped so an over-achieved week doesn't overflow. */
    val weeklyProgress: Float
        get() = if (weeklyGoal <= 0) 0f else (weeklyCompleted.toFloat() / weeklyGoal).coerceIn(0f, 1f)
}

@Singleton
class StatsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    fun observeMyStats(): Flow<UserStats> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(UserStats())
            awaitClose { }
            return@callbackFlow
        }
        val registration = firestore.collection("stats").document(uid)
            .addSnapshotListener { snap, _ ->
                trySend(
                    UserStats(
                        totalWatchMinutes = (snap?.getLong("totalWatchMinutes") ?: 0L).toInt(),
                        moviesCompleted = (snap?.getLong("moviesCompleted") ?: 0L).toInt(),
                        reviewsWritten = (snap?.getLong("reviewsWritten") ?: 0L).toInt(),
                        topGenres = (snap?.get("topGenres") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                        currentStreak = (snap?.getLong("currentStreak") ?: 0L).toInt(),
                        longestStreak = (snap?.getLong("longestStreak") ?: 0L).toInt(),
                        weeklyCompleted = (snap?.getLong("weeklyCompleted") ?: 0L).toInt(),
                        weeklyGoal = (snap?.getLong("weeklyGoal") ?: 3L).toInt(),
                    ),
                )
            }
        awaitClose { registration.remove() }
    }
}
