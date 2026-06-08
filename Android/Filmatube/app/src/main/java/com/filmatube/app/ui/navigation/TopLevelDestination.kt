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
}
