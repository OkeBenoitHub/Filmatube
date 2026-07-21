package com.filmatube.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Breathing room between the outer items' indicators and the screen edges.
 *
 * The ceiling is set by the M3 pill indicator's fixed 64dp width: five of them need 320dp,
 * so a 360dp screen has 40dp spare — 20dp per side — before an item becomes narrower than
 * its own indicator and the outer pills start overlapping their neighbours. 16dp sits just
 * inside that, and every wider phone has more room to spare.
 */
private val EDGE_INSET = 16.dp

/**
 * Switch to a [TopLevelDestination] the way the bottom bar does: reuse the existing entry,
 * keep each tab's own back stack, and don't pile duplicates on top of the current tab.
 *
 * Shared so in-content shortcuts to a tab (the Home header's profile button, say) behave
 * identically to tapping the tab itself — navigating plainly would push a second Profile
 * onto Home's stack, so Back would land you on Home instead of leaving the tab.
 */
fun NavController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Bottom navigation bar across the five [TopLevelDestination]s, themed to the brand:
 * green pill indicator, green selected label, muted unselected state.
 *
 * Uses the standard single-top + save/restore-state pattern so each tab keeps its own
 * back stack and re-selecting a tab returns to its root.
 */
@Composable
fun FilmatubeBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        // Five items split the width evenly, so the outer two sit hard against the screen
        // edges and their pill indicators nearly touch them. Inset the row's *content*
        // via windowInsets rather than Modifier.padding: padding would shrink the bar's
        // surface too, leaving unpainted strips down both sides.
        windowInsets = NavigationBarDefaults.windowInsets.add(
            WindowInsets(left = EDGE_INSET, right = EDGE_INSET),
        ),
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTopLevel(destination.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = stringResource(destination.labelRes),
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
