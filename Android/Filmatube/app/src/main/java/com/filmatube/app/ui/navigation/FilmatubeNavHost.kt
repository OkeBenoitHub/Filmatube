package com.filmatube.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.filmatube.app.ui.browse.BrowseScreen
import com.filmatube.app.ui.community.CommunityScreen
import com.filmatube.app.ui.detail.MovieDetailScreen
import com.filmatube.app.ui.home.HomeScreen
import com.filmatube.app.ui.profile.EditProfileScreen
import com.filmatube.app.ui.profile.ProfileScreen
import com.filmatube.app.ui.search.SearchScreen
import com.filmatube.app.ui.settings.ProfilesScreen
import com.filmatube.app.ui.settings.SettingsScreen
import com.filmatube.app.ui.theater.TheaterScreen

private const val ROUTE_PROFILE_EDIT = "profile/edit"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_PROFILES = "settings/profiles"
private const val ROUTE_MOVIE = "movie/{movieId}"
private const val ROUTE_BROWSE = "browse?genre={genre}"

fun movieRoute(movieId: String) = "movie/$movieId"
fun browseRoute(genre: String?) = if (genre == null) "browse" else "browse?genre=$genre"

/**
 * Top-level navigation graph. Each [TopLevelDestination] maps to one composable screen.
 * Feature-specific sub-graphs (detail, player, auth…) are nested here on later days.
 */
@Composable
fun FilmatubeNavHost(
    navController: NavHostController,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(
                onMovieClick = { navController.navigate(movieRoute(it)) },
                onBrowse = { genre -> navController.navigate(browseRoute(genre)) },
            )
        }
        composable(ROUTE_MOVIE) {
            MovieDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = ROUTE_BROWSE,
            arguments = listOf(navArgument("genre") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) {
            BrowseScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigate(movieRoute(it)) },
            )
        }
        composable(TopLevelDestination.SEARCH.route) { SearchScreen() }
        composable(TopLevelDestination.THEATER.route) { TheaterScreen() }
        composable(TopLevelDestination.COMMUNITY.route) { CommunityScreen() }
        composable(TopLevelDestination.PROFILE.route) {
            ProfileScreen(
                onEditProfile = { navController.navigate(ROUTE_PROFILE_EDIT) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_PROFILE_EDIT) {
            EditProfileScreen(onDone = { navController.popBackStack() })
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onManageProfiles = { navController.navigate(ROUTE_PROFILES) },
                onSignedOut = onSignedOut,
            )
        }
        composable(ROUTE_PROFILES) {
            ProfilesScreen(onBack = { navController.popBackStack() })
        }
    }
}
