package com.filmatube.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filmatube.app.ui.FilmatubeAppRoot
import com.filmatube.app.ui.auth.LoginScreen
import com.filmatube.app.ui.auth.RegisterScreen
import com.filmatube.app.ui.onboarding.OnboardingScreen
import com.filmatube.app.ui.splash.SplashDestination
import com.filmatube.app.ui.splash.SplashScreen

/** Top-level routes above the bottom-nav graph. */
object RootRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
}

/**
 * Root navigation: splash → (onboarding on first run) → auth → main app (bottom-nav scaffold).
 */
@Composable
fun RootNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RootRoutes.SPLASH) {
        composable(RootRoutes.SPLASH) {
            SplashScreen(
                onNavigate = { destination ->
                    val route = when (destination) {
                        SplashDestination.ONBOARDING -> RootRoutes.ONBOARDING
                        SplashDestination.LOGIN -> RootRoutes.LOGIN
                        SplashDestination.MAIN -> RootRoutes.MAIN
                    }
                    navController.navigate(route) {
                        popUpTo(RootRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(RootRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(RootRoutes.LOGIN) {
                        popUpTo(RootRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(RootRoutes.LOGIN) {
            LoginScreen(
                onLoggedIn = { navController.navigateToMain() },
                onNavigateToRegister = { navController.navigate(RootRoutes.REGISTER) },
                onNavigateToForgot = { /* wired on Day 17 */ },
            )
        }

        composable(RootRoutes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigateToMain() },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }

        composable(RootRoutes.MAIN) {
            FilmatubeAppRoot()
        }
    }
}

/** Enters the main app and clears the entire auth/onboarding back stack. */
private fun NavController.navigateToMain() {
    navigate(RootRoutes.MAIN) {
        popUpTo(0) { inclusive = true }
    }
}
