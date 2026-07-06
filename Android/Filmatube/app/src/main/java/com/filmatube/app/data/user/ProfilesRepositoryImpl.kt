package com.filmatube.app.data.user

import com.filmatube.app.domain.model.WatchProfile
import com.filmatube.app.domain.repository.ProfilesRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilesRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ProfilesRepository {

    private fun profiles(uid: String) =
        firestore.collection("users").document(uid).collection("profiles")

    override fun observeProfiles(uid: String): Flow<List<WatchProfile>> = callbackFlow {
        val registration = profiles(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents.orEmpty().map { doc ->
                WatchProfile(
                    id = doc.id,
                    name = doc.getString("name").orEmpty(),
                    avatarEmoji = doc.getString("avatarEmoji") ?: "🍿",
                    isDefault = doc.getBoolean("isDefault") ?: false,
                    language = doc.getString("language") ?: "en",
                )
            }.sortedWith(compareByDescending<WatchProfile> { it.isDefault }.thenBy { it.name.lowercase() })
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun ensureDefaultProfile(uid: String, name: String, language: String) {
        val existing = profiles(uid).limit(1).get().await()
        if (existing.isEmpty) {
            profiles(uid).add(
                mapOf(
                    "name" to name,
                    "avatarEmoji" to "🍿",
                    "isDefault" to true,
                    "language" to language,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    override suspend fun createProfile(uid: String, name: String, avatarEmoji: String) {
        profiles(uid).add(
            mapOf(
                "name" to name,
                "avatarEmoji" to avatarEmoji,
                "isDefault" to false,
                "language" to "en",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override suspend fun updateProfile(uid: String, id: String, name: String, avatarEmoji: String) {
        profiles(uid).document(id).update(
            mapOf("name" to name, "avatarEmoji" to avatarEmoji),
        ).await()
    }

    override suspend fun deleteProfile(uid: String, id: String) {
        profiles(uid).document(id).delete().await()
    }
}
