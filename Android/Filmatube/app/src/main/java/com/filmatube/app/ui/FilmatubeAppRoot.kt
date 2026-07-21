package com.filmatube.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmatube.app.ui.components.FilmatubeSnackbarHost
import com.filmatube.app.ui.navigation.FilmatubeBottomBar
import com.filmatube.app.ui.navigation.FilmatubeNavHost
import com.filmatube.app.ui.navigation.TopLevelDestination

/**
 * Root composable: a [Scaffold] hosting the bottom navigation bar, the app-wide snackbar host,
 * and the top-level nav graph. Mounted by `MainActivity` inside `FilmatubeTheme`.
 */
@Composable
fun FilmatubeAppRoot(onSignedOut: () -> Unit) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: AppRootViewModel = hiltViewModel()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.registerPushToken() }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.registerPushToken()
        }
    }

    // The bar belongs to the five tab roots only. On a pushed screen it kept a tab lit as if
    // that were still where you were, and offered a sideways move when the expected gesture
    // is Back — which is what made deeper navigation feel unmoored.
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = TopLevelDestination.isTopLevel(currentRoute)

    Scaffold(
        bottomBar = {
            // Slides out rather than vanishing, so pushing into a detail screen reads as one
            // continuous motion instead of the layout snapping taller.
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                FilmatubeBottomBar(navController)
            }
        },
        snackbarHost = { FilmatubeSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        FilmatubeNavHost(
            navController = navController,
            onSignedOut = onSignedOut,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // `padding` offsets the content but leaves the insets *unconsumed*, so a
                // screen's own TopAppBar would add the status-bar inset a second time and
                // open a visible gap above its title. Consuming here tells descendants the
                // system bars are already accounted for.
                .consumeWindowInsets(innerPadding),
        )
    }
}
