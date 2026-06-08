package com.filmatube.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.filmatube.app.ui.navigation.FilmatubeBottomBar
import com.filmatube.app.ui.navigation.FilmatubeNavHost

/**
 * Root composable: a [Scaffold] hosting the bottom navigation bar and the top-level nav graph.
 * Mounted by `MainActivity` inside `FilmatubeTheme`.
 */
@Composable
fun FilmatubeAppRoot() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { FilmatubeBottomBar(navController) },
    ) { innerPadding ->
        FilmatubeNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
