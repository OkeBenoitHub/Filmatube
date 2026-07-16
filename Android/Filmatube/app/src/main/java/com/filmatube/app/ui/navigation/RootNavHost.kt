package com.filmatube.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filmatube.app.R
import com.filmatube.app.ui.FilmatubeAppRoot
import com.filmatube.app.ui.auth.AuthNavTarget
import com.filmatube.app.ui.auth.ForgotPasswordScreen
import com.filmatube.app.ui.auth.LoginScreen
import com.filmatube.app.ui.auth.RegisterScreen
import com.filmatube.app.ui.landing.LandingEntryViewModel
import com.filmatube.app.ui.landing.LandingScreen
import com.filmatube.app.ui.onboarding.OnboardingScreen
import com.filmatube.app.ui.splash.SplashDestination
import com.filmatube.app.ui.splash.SplashScreen
import com.filmatube.app.ui.taste.TasteScreen

/** Top-level routes above the bottom-nav graph. */
object RootRoutes {
    const val SPLASH = "splash"
    const val LANDING = "landing"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val TASTE = "taste"
    const val MAIN = "main"
}

/**
 * Root navigation: splash → landing (every signed-out open) → auth → main app.
 *
 * The landing mirrors the web marketing page. "Get started" runs the carousel only on first
 * open (onboardingCompleted); afterwards both CTAs lead to sign-in. Sign-out also returns to
 * the landing, matching the web.
 */
@Composable
fun RootNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RootRoutes.SPLASH) {
        composable(RootRoutes.SPLASH) {
            SplashScreen(
                onNavigate = { destination ->
                    val route = when (destination) {
                        SplashDestination.LANDING -> RootRoutes.LANDING
                        SplashDestination.MAIN -> RootRoutes.MAIN
                    }
                    navController.navigate(route) {
                        popUpTo(RootRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(RootRoutes.LANDING) {
            // Every signed-out open starts here; the get-started carousel only runs once.
            val entryViewModel: LandingEntryViewModel = hiltViewModel()
            val onboardingCompleted by entryViewModel.onboardingCompleted.collectAsStateWithLifecycle()
            LandingScreen(
                // Entry screen — nothing behind it, so no back arrow.
                primaryLabel = R.string.landing_cta_get_started,
                onPrimary = {
                    val route = if (onboardingCompleted == true) RootRoutes.LOGIN else RootRoutes.ONBOARDING
                    navController.navigate(route)
                },
                secondaryLabel = R.string.landing_cta_sign_in,
                onSecondary = { navController.navigate(RootRoutes.LOGIN) },
            )
        }

        composable(RootRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(RootRoutes.LOGIN) {
                        // Drop the carousel; the landing stays beneath login so Back returns to it.
                        popUpTo(RootRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(RootRoutes.LOGIN) {
            LoginScreen(
                onAuthenticated = { navController.onAuthenticated(it) },
                onNavigateToRegister = { navController.navigate(RootRoutes.REGISTER) },
                onNavigateToForgot = { navController.navigate(RootRoutes.FORGOT) },
            )
        }

        composable(RootRoutes.FORGOT) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(RootRoutes.REGISTER) {
            RegisterScreen(
                onAuthenticated = { navController.onAuthenticated(it) },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }

        composable(RootRoutes.TASTE) {
            TasteScreen(onFinished = { navController.navigateClearingBackStack(RootRoutes.MAIN) })
        }

        composable(RootRoutes.MAIN) {
            FilmatubeAppRoot(
                onSignedOut = { navController.navigateClearingBackStack(RootRoutes.LANDING) },
            )
        }
    }
}

/** Route to taste onboarding (new user) or straight to the main app. */
private fun NavController.onAuthenticated(target: AuthNavTarget) {
    val route = when (target) {
        AuthNavTarget.TASTE -> RootRoutes.TASTE
        AuthNavTarget.MAIN -> RootRoutes.MAIN
    }
    navigateClearingBackStack(route)
}

/** Navigate to [route], clearing the entire back stack behind it. */
private fun NavController.navigateClearingBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}
