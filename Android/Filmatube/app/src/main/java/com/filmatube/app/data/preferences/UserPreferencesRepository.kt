package com.filmatube.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide user preferences backed by the Preferences DataStore.
 * Grows over time (language, player prefs, etc.); for now it tracks onboarding.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    private val preferences: Flow<androidx.datastore.preferences.core.Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    /** Whether the user has finished the first-run onboarding flow. */
    val onboardingCompleted: Flow<Boolean> =
        preferences.map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETED] = completed }
    }

    /** The currently selected watch profile id (null if none picked yet). */
    val activeProfileId: Flow<String?> =
        preferences.map { prefs -> prefs[Keys.ACTIVE_PROFILE_ID] }

    suspend fun setActiveProfileId(id: String) {
        dataStore.edit { prefs -> prefs[Keys.ACTIVE_PROFILE_ID] = id }
    }
}
