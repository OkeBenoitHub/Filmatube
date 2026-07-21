package com.filmatube.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.ui.graphics.vector.ImageVector
import com.filmatube.app.R

/**
 * The five primary sections of the app, surfaced in the bottom navigation bar.
 * Each owns a stable [route] used by the Navigation-Compose graph.
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("search", R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    THEATER("theater", R.string.nav_theater, Icons.Filled.Theaters, Icons.Outlined.Theaters),
    COMMUNITY("community", R.string.nav_community, Icons.Filled.Groups, Icons.Outlined.Groups),
    PROFILE("profile", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
    ;

    companion object {
        /**
         * True only for the five tab roots.
         *
         * The bottom bar is a *switcher between these five sections*, so it should only be on
         * screen where switching is what the tap means. On a pushed screen — a movie, a board,
         * a showtime — it kept highlighting whichever tab you came from, which reads as "you
         * are here" when you are in fact one or more levels deeper.
         */
        fun isTopLevel(route: String?): Boolean = entries.any { it.route == route }
    }
}
