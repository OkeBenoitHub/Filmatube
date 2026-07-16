package com.filmatube.app.ui.navigation

import androidx.compose.runtime.Composable
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
 * Root navigation: splash → (landing → get-started on first run) → auth → main app.
 *
 * First open while signed out shows the marketing landing (mirrors the web landing page); its
 * CTAs lead into the get-started carousel or straight to sign-in. Returning signed-out users
 * skip it and land on login.
 */
@Composable
fun RootNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RootRoutes.SPLASH) {
        composable(RootRoutes.SPLASH) {
            SplashScreen(
                onNavigate = { destination ->
                    val route = when (destination) {
                        SplashDestination.ONBOARDING -> RootRoutes.LANDING
                        SplashDestination.LOGIN -> RootRoutes.LOGIN
                        SplashDestination.MAIN -> RootRoutes.MAIN
                    }
                    navController.navigate(route) {
                        popUpTo(RootRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(RootRoutes.LANDING) {
            LandingScreen(
                // Entry screen — nothing behind it, so no back arrow.
                primaryLabel = R.string.landing_cta_get_started,
                onPrimary = { navController.navigate(RootRoutes.ONBOARDING) },
                secondaryLabel = R.string.landing_cta_sign_in,
                onSecondary = {
                    navController.navigate(RootRoutes.LOGIN) {
                        popUpTo(RootRoutes.LANDING) { inclusive = true }
                    }
                },
            )
        }

        composable(RootRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(RootRoutes.LOGIN) {
                        // Clear the landing too, so Back from login doesn't re-enter the funnel.
                        popUpTo(RootRoutes.LANDING) { inclusive = true }
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
                onSignedOut = { navController.navigateClearingBackStack(RootRoutes.LOGIN) },
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
