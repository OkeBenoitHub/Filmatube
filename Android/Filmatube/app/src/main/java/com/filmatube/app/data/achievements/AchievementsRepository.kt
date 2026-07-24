package com.filmatube.app.data.achievements

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the user's unlocked badges (`achievements/{uid}/badges`). Awarded server-side by Cloud
 * Functions (Day 200); read-only here.
 */
@Singleton
class AchievementsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    fun observeMyBadges(): Flow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }
        val registration = firestore.collection("achievements").document(uid).collection("badges")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.map { it.id }?.toSet() ?: emptySet())
            }
        awaitClose { registration.remove() }
    }
}
