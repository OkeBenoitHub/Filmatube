package com.filmatube.app.data.user

import com.filmatube.app.domain.model.AuthUser
import com.filmatube.app.domain.model.UserProfile
import com.filmatube.app.domain.repository.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    override fun observeUser(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toUserProfile())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun ensureUserDocument(user: AuthUser) {
        val ref = firestore.collection(USERS).document(user.uid)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            ref.set(defaultUserData(user)).await()
        } else {
            ref.update(FIELD_LAST_ACTIVE, FieldValue.serverTimestamp()).await()
        }
    }

    override suspend fun needsTasteOnboarding(uid: String): Boolean {
        val snapshot = firestore.collection(USERS).document(uid).get().await()
        return snapshot.getBoolean("tasteCompleted") != true
    }

    override suspend fun saveTaste(
        uid: String,
        genres: List<String>,
        contentLanguage: String,
        language: String,
    ) {
        firestore.collection(USERS).document(uid).update(
            mapOf(
                "genrePreferences" to genres,
                "contentLanguage" to contentLanguage,
                "language" to language,
                "tasteCompleted" to true,
                FIELD_LAST_ACTIVE to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun defaultUserData(user: AuthUser): Map<String, Any?> {
        val language = if (Locale.getDefault().language == "fr") "fr" else "en"
        return mapOf(
            "email" to user.email,
            "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Filmatube user"),
            "bio" to "",
            "avatarUrl" to (user.photoUrl ?: ""),
            "language" to language,
            "followersCount" to 0L,
            "followingCount" to 0L,
            "genrePreferences" to emptyList<String>(),
            "contentLanguage" to "both",
            "tasteCompleted" to false,
            "isAdmin" to false,
            "isBanned" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            FIELD_LAST_ACTIVE to FieldValue.serverTimestamp(),
        )
    }

    override suspend fun getUser(uid: String): UserProfile? =
        firestore.collection(USERS).document(uid).get().await().toUserProfile()

    override suspend fun updateProfile(uid: String, displayName: String, bio: String) {
        firestore.collection(USERS).document(uid).update(
            mapOf(
                "displayName" to displayName,
                "bio" to bio,
                FIELD_LAST_ACTIVE to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override suspend fun updateAvatarUrl(uid: String, avatarUrl: String) {
        firestore.collection(USERS).document(uid).update("avatarUrl", avatarUrl).await()
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile? {
        if (!exists()) return null
        return UserProfile(
            uid = id,
            email = getString("email"),
            displayName = getString("displayName").orEmpty(),
            bio = getString("bio").orEmpty(),
            avatarUrl = getString("avatarUrl").orEmpty(),
            language = getString("language") ?: "en",
            followersCount = getLong("followersCount") ?: 0L,
            followingCount = getLong("followingCount") ?: 0L,
            isAdmin = getBoolean("isAdmin") ?: false,
        )
    }

    private companion object {
        const val USERS = "users"
        const val FIELD_LAST_ACTIVE = "lastActiveAt"
    }
}
