package com.filmatube.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.filmatube.app.ui.community.CommunityScreen
import com.filmatube.app.ui.home.HomeScreen
import com.filmatube.app.ui.profile.EditProfileScreen
import com.filmatube.app.ui.profile.ProfileScreen
import com.filmatube.app.ui.search.SearchScreen
import com.filmatube.app.ui.theater.TheaterScreen

private const val ROUTE_PROFILE_EDIT = "profile/edit"

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
        composable(TopLevelDestination.HOME.route) { HomeScreen() }
        composable(TopLevelDestination.SEARCH.route) { SearchScreen() }
        composable(TopLevelDestination.THEATER.route) { TheaterScreen() }
        composable(TopLevelDestination.COMMUNITY.route) { CommunityScreen() }
        composable(TopLevelDestination.PROFILE.route) {
            ProfileScreen(
                onEditProfile = { navController.navigate(ROUTE_PROFILE_EDIT) },
                onSignedOut = onSignedOut,
            )
        }
        composable(ROUTE_PROFILE_EDIT) {
            EditProfileScreen(onDone = { navController.popBackStack() })
        }
    }
}
