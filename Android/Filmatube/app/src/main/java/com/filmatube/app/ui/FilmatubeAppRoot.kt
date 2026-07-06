package com.filmatube.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.filmatube.app.ui.components.FilmatubeSnackbarHost
import com.filmatube.app.ui.navigation.FilmatubeBottomBar
import com.filmatube.app.ui.navigation.FilmatubeNavHost

/**
 * Root composable: a [Scaffold] hosting the bottom navigation bar, the app-wide snackbar host,
 * and the top-level nav graph. Mounted by `MainActivity` inside `FilmatubeTheme`.
 */
@Composable
fun FilmatubeAppRoot(onSignedOut: () -> Unit) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        bottomBar = { FilmatubeBottomBar(navController) },
        snackbarHost = { FilmatubeSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        FilmatubeNavHost(
            navController = navController,
            onSignedOut = onSignedOut,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
