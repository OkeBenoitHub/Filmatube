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
import com.filmatube.app.R
import com.filmatube.app.domain.repository.MovieSort
import com.filmatube.app.ui.browse.BrowseScreen
import com.filmatube.app.ui.components.MovieActionHandlers
import com.filmatube.app.ui.components.ProvideMovieActions
import com.filmatube.app.ui.boards.BoardDetailScreen
import com.filmatube.app.ui.boards.BoardsScreen
import com.filmatube.app.ui.boards.CreateBoardScreen
import com.filmatube.app.ui.boards.MembersScreen
import com.filmatube.app.ui.parties.CreatePartyScreen
import com.filmatube.app.ui.parties.PartyScreen
import com.filmatube.app.ui.community.CommunityScreen
import com.filmatube.app.ui.social.CommentsScreen
import com.filmatube.app.ui.detail.ActorScreen
import com.filmatube.app.ui.detail.MovieDetailScreen
import com.filmatube.app.ui.downloads.DownloadsScreen
import com.filmatube.app.ui.home.HomeScreen
import com.filmatube.app.ui.landing.LandingScreen
import com.filmatube.app.ui.notifications.NotificationCenterScreen
import com.filmatube.app.ui.notifications.NotificationPreferencesScreen
import com.filmatube.app.ui.collections.CollectionDetailScreen
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
import com.filmatube.app.ui.theater.ShowtimeScreen
import com.filmatube.app.ui.theater.TheaterScreen

private const val ROUTE_PROFILE_EDIT = "profile/edit"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_PROFILES = "settings/profiles"
private const val ROUTE_DOWNLOADS = "downloads"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_COLLECTION = "collection/{collectionId}"
private const val ROUTE_FOLLOWS = "follows/{mode}"
private const val ROUTE_SUGGESTIONS = "suggestions"
private const val ROUTE_INBOX = "inbox"
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_NOTIFICATION_PREFS = "settings/notifications"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_PUBLIC_PROFILE = "user/{userId}"
private const val ROUTE_REVIEWS = "reviews/{movieId}"
private const val ROUTE_COMMENTS = "comments/{movieId}"
private const val ROUTE_BOARDS = "boards"
private const val ROUTE_BOARD = "board/{boardId}"
private const val ROUTE_CREATE_BOARD = "boards/create"
private const val ROUTE_BOARD_MEMBERS = "board/{boardId}/members"
fun boardRoute(boardId: String) = "board/$boardId"
fun boardMembersRoute(boardId: String) = "board/$boardId/members"

private const val ROUTE_PARTY = "party/{partyId}"
private const val ROUTE_CREATE_PARTY = "party/create/{movieId}"
fun partyRoute(partyId: String) = "party/$partyId"
fun createPartyRoute(movieId: String) = "party/create/$movieId"

private const val ROUTE_SHOWTIME = "showtime/{showtimeId}"
fun showtimeRoute(showtimeId: String) = "showtime/$showtimeId"

fun followsRoute(mode: String) = "follows/$mode"
fun publicProfileRoute(userId: String) = "user/$userId"
fun reviewsRoute(movieId: String) = "reviews/$movieId"
fun commentsRoute(movieId: String) = "comments/$movieId"
private const val ROUTE_MOVIE = "movie/{movieId}"
private const val ROUTE_PLAYER = "player/{movieId}?party={party}&showtime={showtime}"
private const val ROUTE_BROWSE = "browse?genre={genre}&sort={sort}&comingSoon={comingSoon}"
private const val ROUTE_ACTOR = "actor/{name}"

fun movieRoute(movieId: String) = "movie/$movieId"
fun collectionRoute(collectionId: String) = "collection/$collectionId"
fun playerRoute(movieId: String) = "player/$movieId"
fun partyPlayerRoute(movieId: String, partyId: String) = "player/$movieId?party=$partyId"
fun theaterPlayerRoute(movieId: String, showtimeId: String) = "player/$movieId?showtime=$showtimeId"
/**
 * Which slice of the catalog a "See all" should open.
 *
 * Every one of Home's rows used to call `onBrowse(null)` or pass only a genre, so Trending,
 * New Releases and Coming Soon all landed on the same unfiltered grid with nothing to tell
 * them apart. Carrying the row's own query fixes that.
 */
data class BrowseTarget(
    val genre: String? = null,
    val sort: MovieSort? = null,
    val comingSoon: Boolean = false,
)

fun browseRoute(target: BrowseTarget = BrowseTarget()): String = buildString {
    append("browse?")
    target.genre?.let { append("genre=$it&") }
    target.sort?.let { append("sort=${it.name}&") }
    if (target.comingSoon) append("comingSoon=true")
}
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
    // Every poster tile in the app can open a movie options sheet; the sheet's navigation is
    // provided here so the six screens that render tiles don't each have to thread it down.
    ProvideMovieActions(
        MovieActionHandlers(
            onPlay = { navController.navigate(playerRoute(it)) },
            onOpenDetails = { navController.navigate(movieRoute(it)) },
            onCreateParty = { navController.navigate(createPartyRoute(it)) },
        ),
    ) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(
                onMovieClick = { navController.navigate(movieRoute(it)) },
                onBrowse = { target -> navController.navigate(browseRoute(target)) },
                onPlay = { navController.navigate(playerRoute(it)) },
                onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                // Profile is a bottom-nav tab, so switch tabs rather than stacking a copy.
                onOpenProfile = { navController.navigateToTopLevel(TopLevelDestination.PROFILE.route) },
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
                onCreateParty = { navController.navigate(createPartyRoute(it)) },
            )
        }
        composable(
            route = ROUTE_PLAYER,
            arguments = listOf(
                navArgument("movieId") { type = NavType.StringType },
                navArgument("party") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("showtime") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
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
            arguments = listOf(
                navArgument("genre") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("sort") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("comingSoon") { type = NavType.BoolType; defaultValue = false },
            ),
        ) {
            BrowseScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigate(movieRoute(it)) },
            )
        }
        composable(TopLevelDestination.SEARCH.route) {
            SearchScreen(
                onMovieClick = { navController.navigate(movieRoute(it)) },
                onBrowse = { navController.navigate(browseRoute()) },
            )
        }
        composable(TopLevelDestination.THEATER.route) {
            TheaterScreen(onShowtimeClick = { navController.navigate(showtimeRoute(it)) })
        }
        composable(
            route = ROUTE_SHOWTIME,
            arguments = listOf(navArgument("showtimeId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "filmatube://showtime/{showtimeId}" }),
        ) {
            ShowtimeScreen(
                onBack = { navController.popBackStack() },
                onEnterTheater = { movieId, showtimeId ->
                    navController.navigate(theaterPlayerRoute(movieId, showtimeId))
                },
            )
        }
        composable(TopLevelDestination.COMMUNITY.route) {
            FeedScreen(
                onMovieClick = { navController.navigate(movieRoute(it)) },
                onUserClick = { navController.navigate(publicProfileRoute(it)) },
                onOpenBoards = { navController.navigate(ROUTE_BOARDS) },
                onOpenParty = { navController.navigate(partyRoute(it)) },
            )
        }
        composable(TopLevelDestination.PROFILE.route) {
            ProfileScreen(
                onEditProfile = { navController.navigate(ROUTE_PROFILE_EDIT) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                onOpenLibrary = { navController.navigate(ROUTE_LIBRARY) },
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
                onOpenNotificationPrefs = { navController.navigate(ROUTE_NOTIFICATION_PREFS) },
                onOpenAbout = { navController.navigate(ROUTE_ABOUT) },
                onSignedOut = onSignedOut,
            )
        }
        composable(ROUTE_ABOUT) {
            // Signed-in context: back arrow + in-app CTAs (the signed-out entry copy differs).
            LandingScreen(
                onBack = { navController.popBackStack() },
                primaryLabel = R.string.landing_cta_primary,
                onPrimary = { navController.navigate(TopLevelDestination.HOME.route) },
                secondaryLabel = R.string.landing_cta_secondary,
                onSecondary = { navController.navigate(TopLevelDestination.COMMUNITY.route) },
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
                onCollectionClick = { navController.navigate(collectionRoute(it)) },
            )
        }
        composable(
            route = ROUTE_COLLECTION,
            arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
        ) {
            CollectionDetailScreen(
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
                onOpenBoard = { navController.navigate(boardRoute(it)) },
                onOpenParty = { navController.navigate(partyRoute(it)) },
                onOpenShowtime = { navController.navigate(showtimeRoute(it)) },
            )
        }
        composable(ROUTE_NOTIFICATION_PREFS) {
            NotificationPreferencesScreen(onBack = { navController.popBackStack() })
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
        composable(ROUTE_BOARDS) {
            BoardsScreen(
                onBoardClick = { navController.navigate(boardRoute(it)) },
                onCreateBoard = { navController.navigate(ROUTE_CREATE_BOARD) },
            )
        }
        composable(ROUTE_CREATE_BOARD) {
            CreateBoardScreen(
                onBack = { navController.popBackStack() },
                onCreated = { boardId ->
                    navController.navigate(boardRoute(boardId)) {
                        popUpTo(ROUTE_CREATE_BOARD) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = ROUTE_BOARD,
            arguments = listOf(navArgument("boardId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "filmatube://board/{boardId}" }),
        ) { entry ->
            val boardId = entry.arguments?.getString("boardId").orEmpty()
            BoardDetailScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigate(movieRoute(it)) },
                onOpenMembers = { navController.navigate(boardMembersRoute(boardId)) },
            )
        }
        composable(
            route = ROUTE_BOARD_MEMBERS,
            arguments = listOf(navArgument("boardId") { type = NavType.StringType }),
        ) {
            MembersScreen(
                onBack = { navController.popBackStack() },
                onUserClick = { navController.navigate(publicProfileRoute(it)) },
            )
        }
        composable(
            route = ROUTE_CREATE_PARTY,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType }),
        ) {
            CreatePartyScreen(
                onBack = { navController.popBackStack() },
                onCreated = { partyId ->
                    navController.navigate(partyRoute(partyId)) {
                        popUpTo(ROUTE_CREATE_PARTY) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = ROUTE_PARTY,
            arguments = listOf(navArgument("partyId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "filmatube://party/{partyId}" }),
        ) {
            PartyScreen(
                onBack = { navController.popBackStack() },
                onWatch = { movieId, partyId -> navController.navigate(partyPlayerRoute(movieId, partyId)) },
            )
        }
    }
    }
}
