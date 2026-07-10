package com.filmatube.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import android.net.Uri
import com.filmatube.app.ui.browse.BrowseScreen
import com.filmatube.app.ui.community.CommunityScreen
import com.filmatube.app.ui.social.CommentsScreen
import com.filmatube.app.ui.detail.ActorScreen
import com.filmatube.app.ui.detail.MovieDetailScreen
import com.filmatube.app.ui.downloads.DownloadsScreen
import com.filmatube.app.ui.home.HomeScreen
import com.filmatube.app.ui.notifications.NotificationCenterScreen
import com.filmatube.app.ui.library.LibraryScreen
import com.filmatube.app.ui.player.PlayerScreen
import com.filmatube.app.ui.profile.EditProfileScreen
import com.filmatube.app.ui.profile.ProfileScreen
import com.filmatube.app.ui.search.SearchScreen
import com.filmatube.app.ui.social.FeedScreen
import com.filmatube.app.ui.social.FollowListScreen
import com.filmatube.app.ui.social.PublicProfileScreen
import com.filmatube.app.ui.social.RecommendationInboxScreen
import com.filmatube.app.ui.social.ReviewsScreen
import com.filmatube.app.ui.social.SuggestionsScreen
import com.filmatube.app.ui.settings.ProfilesScreen
import com.filmatube.app.ui.settings.SettingsScreen
import com.filmatube.app.ui.theater.TheaterScreen

private const val ROUTE_PROFILE_EDIT = "profile/edit"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_PROFILES = "settings/profiles"
private const val ROUTE_DOWNLOADS = "downloads"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_FOLLOWS = "follows/{mode}"
private const val ROUTE_SUGGESTIONS = "suggestions"
private const val ROUTE_INBOX = "inbox"
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_PUBLIC_PROFILE = "user/{userId}"
private const val ROUTE_REVIEWS = "reviews/{movieId}"
private const val ROUTE_COMMENTS = "comments/{movieId}"

fun followsRoute(mode: String) = "follows/$mode"
fun publicProfileRoute(userId: String) = "user/$userId"
fun reviewsRoute(movieId: String) = "reviews/$movieId"
fun commentsRoute(movieId: String) = "comments/$movieId"
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
        composable(
            route = ROUTE_MOVIE,
            deepLinks = listOf(navDeepLink { uriPattern = "filmatube://movie/{movieId}" }),
        ) {
            MovieDetailScreen(
                onBack = { navController.popBackStack() },
                onPlay = { navController.navigate(playerRoute(it)) },
                onMovieClick = { navController.navigate(movieRoute(it)) },
                onActorClick = { navController.navigate(actorRoute(it)) },
                onOpenReviews = { navController.navigate(reviewsRoute(it)) },
                onOpenComments = { navController.navigate(commentsRoute(it)) },
            )
        }
        composable(
            route = ROUTE_PLAYER,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "filmatube://watch/{movieId}" }),
        ) {
            PlayerScreen(
                onBack = { navController.popBackStack() },
                onPlayNext = { nextId ->
                    navController.navigate(playerRoute(nextId)) {
                        popUpTo(ROUTE_PLAYER) { inclusive = true }
                    }
                },
            )
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
        composable(TopLevelDestination.COMMUNITY.route) {
            FeedScreen(
                onMovieClick = { navController.navigate(movieRoute(it)) },
                onUserClick = { navController.navigate(publicProfileRoute(it)) },
            )
        }
        composable(TopLevelDestination.PROFILE.route) {
            ProfileScreen(
                onEditProfile = { navController.navigate(ROUTE_PROFILE_EDIT) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onOpenFollowers = { navController.navigate(followsRoute("followers")) },
                onOpenFollowing = { navController.navigate(followsRoute("following")) },
                onOpenSuggestions = { navController.navigate(ROUTE_SUGGESTIONS) },
                onOpenInbox = { navController.navigate(ROUTE_INBOX) },
                onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
            )
        }
        composable(ROUTE_PROFILE_EDIT) {
            EditProfileScreen(onDone = { navController.popBackStack() })
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onManageProfiles = { navController.navigate(ROUTE_PROFILES) },
                onOpenDownloads = { navController.navigate(ROUTE_DOWNLOADS) },
                onOpenLibrary = { navController.navigate(ROUTE_LIBRARY) },
                onSignedOut = onSignedOut,
            )
        }
        composable(ROUTE_PROFILES) {
            ProfilesScreen(onBack = { navController.popBackStack() })
        }
        composable(ROUTE_DOWNLOADS) {
            DownloadsScreen(onBack = { navController.popBackStack() })
        }
        composable(ROUTE_LIBRARY) {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigate(movieRoute(it)) },
            )
        }
        composable(
            route = ROUTE_FOLLOWS,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) {
            FollowListScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { navController.navigate(publicProfileRoute(it)) },
            )
        }
        composable(ROUTE_SUGGESTIONS) {
            SuggestionsScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { navController.navigate(publicProfileRoute(it)) },
            )
        }
        composable(ROUTE_INBOX) {
            RecommendationInboxScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigate(movieRoute(it)) },
            )
        }
        composable(ROUTE_NOTIFICATIONS) {
            NotificationCenterScreen(
                onBack = { navController.popBackStack() },
                onOpenMovie = { navController.navigate(movieRoute(it)) },
                onOpenUser = { navController.navigate(publicProfileRoute(it)) },
            )
        }
        composable(
            route = ROUTE_PUBLIC_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) {
            PublicProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = ROUTE_REVIEWS,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
        ) {
            ReviewsScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { navController.navigate(publicProfileRoute(it)) },
            )
        }
        composable(
            route = ROUTE_COMMENTS,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
        ) {
            CommentsScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { navController.navigate(publicProfileRoute(it)) },
            )
        }
    }
}
