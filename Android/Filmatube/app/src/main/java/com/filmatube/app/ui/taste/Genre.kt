package com.filmatube.app.ui.taste

import androidx.annotation.StringRes
import com.filmatube.app.R

/**
 * Canonical movie genres. [key] is the stable value stored in Firestore
 * (`genrePreferences`) and used for catalog queries; [labelRes] is the localized label.
 */
enum class Genre(val key: String, @StringRes val labelRes: Int) {
    ACTION("action", R.string.genre_action),
    ADVENTURE("adventure", R.string.genre_adventure),
    ANIMATION("animation", R.string.genre_animation),
    COMEDY("comedy", R.string.genre_comedy),
    CRIME("crime", R.string.genre_crime),
    DOCUMENTARY("documentary", R.string.genre_documentary),
    DRAMA("drama", R.string.genre_drama),
    FAMILY("family", R.string.genre_family),
    FANTASY("fantasy", R.string.genre_fantasy),
    HISTORY("history", R.string.genre_history),
    HORROR("horror", R.string.genre_horror),
    MUSIC("music", R.string.genre_music),
    MYSTERY("mystery", R.string.genre_mystery),
    ROMANCE("romance", R.string.genre_romance),
    SCIFI("scifi", R.string.genre_scifi),
    THRILLER("thriller", R.string.genre_thriller),
    WAR("war", R.string.genre_war),
    WESTERN("western", R.string.genre_western),
}
