package com.filmatube.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.filmatube.app.domain.model.DownloadQuality
import com.filmatube.app.domain.model.SubtitleBackground
import com.filmatube.app.domain.model.SubtitleEdge
import com.filmatube.app.domain.model.SubtitlePosition
import com.filmatube.app.domain.model.SubtitleSize
import com.filmatube.app.domain.model.SubtitleStyle
import com.filmatube.app.domain.model.SubtitleTextColor
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
        val SUB_SIZE = stringPreferencesKey("subtitle_size")
        val SUB_TEXT_COLOR = stringPreferencesKey("subtitle_text_color")
        val SUB_BACKGROUND = stringPreferencesKey("subtitle_background")
        val SUB_EDGE = stringPreferencesKey("subtitle_edge")
        val SUB_POSITION = stringPreferencesKey("subtitle_position")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        val DOWNLOAD_AUTO_DELETE_WATCHED = booleanPreferencesKey("download_auto_delete_watched")
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

    /** Subtitle appearance, applied by the player's SubtitleView. */
    val subtitleStyle: Flow<SubtitleStyle> = preferences.map { prefs ->
        SubtitleStyle(
            size = prefs[Keys.SUB_SIZE].toEnum(SubtitleSize.MEDIUM) { SubtitleSize.valueOf(it) },
            textColor = prefs[Keys.SUB_TEXT_COLOR].toEnum(SubtitleTextColor.WHITE) { SubtitleTextColor.valueOf(it) },
            background = prefs[Keys.SUB_BACKGROUND].toEnum(SubtitleBackground.NONE) { SubtitleBackground.valueOf(it) },
            edge = prefs[Keys.SUB_EDGE].toEnum(SubtitleEdge.SHADOW) { SubtitleEdge.valueOf(it) },
            position = prefs[Keys.SUB_POSITION].toEnum(SubtitlePosition.NORMAL) { SubtitlePosition.valueOf(it) },
        )
    }

    suspend fun setSubtitleStyle(style: SubtitleStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.SUB_SIZE] = style.size.name
            prefs[Keys.SUB_TEXT_COLOR] = style.textColor.name
            prefs[Keys.SUB_BACKGROUND] = style.background.name
            prefs[Keys.SUB_EDGE] = style.edge.name
            prefs[Keys.SUB_POSITION] = style.position.name
        }
    }

    /** Preferred download quality. */
    val downloadQuality: Flow<DownloadQuality> = preferences.map { prefs ->
        prefs[Keys.DOWNLOAD_QUALITY].toEnum(DownloadQuality.STANDARD) { DownloadQuality.valueOf(it) }
    }

    suspend fun setDownloadQuality(quality: DownloadQuality) {
        dataStore.edit { prefs -> prefs[Keys.DOWNLOAD_QUALITY] = quality.name }
    }

    /** Whether downloads are restricted to un-metered (Wi-Fi) networks. */
    val downloadWifiOnly: Flow<Boolean> = preferences.map { prefs -> prefs[Keys.DOWNLOAD_WIFI_ONLY] ?: false }

    suspend fun setDownloadWifiOnly(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DOWNLOAD_WIFI_ONLY] = enabled }
    }

    /** Auto-remove a download once the movie has been watched (≥90%). */
    val downloadAutoDeleteWatched: Flow<Boolean> =
        preferences.map { prefs -> prefs[Keys.DOWNLOAD_AUTO_DELETE_WATCHED] ?: false }

    suspend fun setDownloadAutoDeleteWatched(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DOWNLOAD_AUTO_DELETE_WATCHED] = enabled }
    }

    private fun <T> String?.toEnum(default: T, parse: (String) -> T): T =
        this?.let { runCatching { parse(it) }.getOrNull() } ?: default
}
