package com.filmatube.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val REMINDERS = stringSetPreferencesKey("coming_soon_reminders")
    }

    private companion object {
        const val MAX_RECENT_SEARCHES = 8
        const val SEPARATOR = "\n"
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

    /** Recent search terms, most-recent first. */
    val recentSearches: Flow<List<String>> = preferences.map { prefs ->
        prefs[Keys.RECENT_SEARCHES]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun addRecentSearch(term: String) {
        val clean = term.trim()
        if (clean.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) })
                .take(MAX_RECENT_SEARCHES)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString(SEPARATOR)
        }
    }

    suspend fun clearRecentSearches() {
        dataStore.edit { prefs -> prefs.remove(Keys.RECENT_SEARCHES) }
    }

    /** Movie ids the user asked to be reminded about (coming-soon). */
    val reminders: Flow<Set<String>> = preferences.map { prefs -> prefs[Keys.REMINDERS] ?: emptySet() }

    suspend fun toggleReminder(movieId: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.REMINDERS] ?: emptySet()
            prefs[Keys.REMINDERS] = if (movieId in current) current - movieId else current + movieId
        }
    }
}
