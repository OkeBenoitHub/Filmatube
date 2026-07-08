package com.filmatube.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.net.Uri
import com.filmatube.app.ui.browse.BrowseScreen
import com.filmatube.app.ui.community.CommunityScreen
import com.filmatube.app.ui.detail.ActorScreen
import com.filmatube.app.ui.detail.MovieDetailScreen
import com.filmatube.app.ui.home.HomeScreen
import com.filmatube.app.ui.player.PlayerScreen
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
private const val ROUTE_PLAYER = "player/{movieId}"
private const val ROUTE_BROWSE = "browse?genre={genre}"
private const val ROUTE_ACTOR = "actor/{name}"

fun movieRoute(movieId: String) = "movie/$movieId"
fun playerRoute(movieId: String) = "player/$movieId"
fun browseRoute(genre: String?) = if (genre == null) "browse" else "browse?genre=$genre"
fun actorRoute(name: String) = "actor/${Uri.encode(name)}"

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
                onPlay = { navController.navigate(playerRoute(it)) },
            )
        }
        composable(ROUTE_MOVIE) {
            MovieDetailScreen(
                onBack = { navController.popBackStack() },
                onPlay = { navController.navigate(playerRoute(it)) },
                onMovieClick = { navController.navigate(movieRoute(it)) },
                onActorClick = { navController.navigate(actorRoute(it)) },
            )
        }
        composable(
            route = ROUTE_PLAYER,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
        ) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = ROUTE_ACTOR,
            arguments = listOf(navArgument("name") { type = NavType.StringType }),
        ) {
            ActorScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigate(movieRoute(it)) },
            )
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
        composable(TopLevelDestination.SEARCH.route) {
            SearchScreen(onMovieClick = { navController.navigate(movieRoute(it)) })
        }
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
