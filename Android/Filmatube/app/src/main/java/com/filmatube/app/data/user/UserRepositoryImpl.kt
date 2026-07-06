package com.filmatube.app.data.user

import com.filmatube.app.domain.model.AuthUser
import com.filmatube.app.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    override suspend fun ensureUserDocument(user: AuthUser) {
        val ref = firestore.collection(USERS).document(user.uid)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            ref.set(defaultUserData(user)).await()
        } else {
            ref.update(FIELD_LAST_ACTIVE, FieldValue.serverTimestamp()).await()
        }
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
            "isAdmin" to false,
            "isBanned" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            FIELD_LAST_ACTIVE to FieldValue.serverTimestamp(),
        )
    }

    private companion object {
        const val USERS = "users"
        const val FIELD_LAST_ACTIVE = "lastActiveAt"
    }
}
