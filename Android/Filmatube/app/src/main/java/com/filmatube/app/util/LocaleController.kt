package com.filmatube.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies and reads the app's UI language via the AndroidX per-app language API.
 * Setting a new locale recreates activities and is persisted automatically
 * (autoStoreLocales on API < 33, framework LocaleManager on 33+).
 */
object LocaleController {

    /** Apply a language tag ("en" / "fr"). No-op if already active. */
    fun apply(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    /** Current app language tag, defaulting to "en" when following the system. */
    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return "en"
        val tag = locales.toLanguageTags().substringBefore(",").substringBefore("-")
        return tag.ifBlank { "en" }
    }
}
