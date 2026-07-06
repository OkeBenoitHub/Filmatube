package com.filmatube.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filmatube.app.ui.FilmatubeAppRoot
import com.filmatube.app.ui.onboarding.OnboardingScreen
import com.filmatube.app.ui.splash.SplashScreen

/** Top-level routes above the bottom-nav graph. */
object RootRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
}

/**
 * Root navigation: splash → (onboarding on first run) → main app (bottom-nav scaffold).
 * The auth flow (login/register) is inserted here on Day 16.
 */
@Composable
fun RootNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RootRoutes.SPLASH) {
        composable(RootRoutes.SPLASH) {
            SplashScreen(
                onNavigate = { onboardingCompleted ->
                    val target = if (onboardingCompleted) RootRoutes.MAIN else RootRoutes.ONBOARDING
                    navController.navigate(target) {
                        popUpTo(RootRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(RootRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(RootRoutes.MAIN) {
                        popUpTo(RootRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(RootRoutes.MAIN) {
            FilmatubeAppRoot()
        }
    }
}
