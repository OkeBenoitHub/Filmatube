package com.filmatube.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.filmatube.app.ui.FilmatubeAppRoot
import com.filmatube.app.ui.theme.FilmatubeTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity that hosts the entire Compose UI inside the forced-dark green theme.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FilmatubeTheme {
                FilmatubeAppRoot()
            }
        }
    }
}
