package com.filmatube.app.domain.repository

import com.filmatube.app.domain.model.WatchProfile
import kotlinx.coroutines.flow.Flow

/** Watch-profile CRUD under `users/{uid}/profiles`. */
interface ProfilesRepository {
    fun observeProfiles(uid: String): Flow<List<WatchProfile>>

    /** Creates a default profile if the account has none yet. */
    suspend fun ensureDefaultProfile(uid: String, name: String, language: String)

    suspend fun createProfile(uid: String, name: String, avatarEmoji: String)

    suspend fun updateProfile(uid: String, id: String, name: String, avatarEmoji: String)

    suspend fun deleteProfile(uid: String, id: String)
}
