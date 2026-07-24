package com.filmatube.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.ui.navigation.RootNavHost
import com.filmatube.app.ui.theme.FilmatubeTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * Single Activity that hosts the entire Compose UI inside the forced-dark green theme.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Fetched via EntryPointAccessors rather than @Inject field injection: member-injecting into
    // an @AndroidEntryPoint class trips Hilt's Java AP here ("unsupported metadata version"), the
    // same reason the messaging service uses this pattern.
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface InviteEntryPoint {
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    /** Whether we're in Picture-in-Picture — read by the player to hide its controls. */
    var isInPipMode by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureInvite(intent)
        setContent {
            FilmatubeTheme {
                RootNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureInvite(intent)
    }

    /**
     * `filmatube://invite/{code}` — stash the code so the sign-up that follows can be attributed
     * (ReferralRepository.attributePendingInvite). Doesn't navigate; the invitee continues to the
     * app's normal start destination (splash → auth).
     */
    private fun captureInvite(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "filmatube" && data.host == "invite") {
            val code = data.lastPathSegment?.takeIf { it.isNotBlank() } ?: return
            val prefs = EntryPointAccessors
                .fromApplication(applicationContext, InviteEntryPoint::class.java)
                .userPreferencesRepository()
            lifecycleScope.launch { prefs.setPendingInviteCode(code) }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}
