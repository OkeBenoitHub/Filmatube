package com.filmatube.app.domain.model

/** A string with per-language variants; falls back to English. */
data class LocalizedText(val en: String = "", val fr: String = "") {
    fun get(language: String): String =
        if (language == "fr" && fr.isNotBlank()) fr else en.ifBlank { fr }
}

/** A subtitle track (.vtt in R2), keyed by BCP-47 language code (en, fr). */
data class SubtitleTrack(val lang: String, val url: String)

/** A movie from the `movies` collection (UI-facing subset). */
data class Movie(
    val id: String,
    val title: LocalizedText,
    val description: LocalizedText,
    val posterUrl: String,
    val backdropUrl: String,
    val trailerUrl: String?,
    val genres: List<String>,
    val year: Int,
    val duration: Int, // minutes
    val ageRating: String,
    val cast: List<CastMember>,
    val directors: List<String>,
    val averageRating: Double,
    val ratingsCount: Long,
    val likesCount: Long,
    val viewsCount: Long,
    val isFeatured: Boolean,
    val isComingSoon: Boolean,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
)
