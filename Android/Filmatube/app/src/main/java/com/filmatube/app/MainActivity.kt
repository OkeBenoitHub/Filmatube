package com.filmatube.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.filmatube.app.ui.theme.FilmatubeTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity that hosts the entire Compose UI.
 *
 * For now it renders a placeholder. The forced-dark green Material 3 theme arrives on Day 2,
 * and the Navigation-Compose graph + bottom navigation on Day 4.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FilmatubeTheme {
                FilmatubeRoot()
            }
        }
    }
}

@Composable
private fun FilmatubeRoot() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Filmatube",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilmatubeRootPreview() {
    FilmatubeTheme {
        FilmatubeRoot()
    }
}
