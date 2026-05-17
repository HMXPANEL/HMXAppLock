package com.hmx.shield.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hmx.shield.features.applock.presentation.screen.AppLockListScreen
import com.hmx.shield.features.applock.presentation.screen.AppLockConfigScreen
import com.hmx.shield.features.dashboard.presentation.screen.DashboardScreen
import com.hmx.shield.features.onboarding.presentation.screen.OnboardingScreen
import com.hmx.shield.features.onboarding.presentation.screen.WelcomeScreen
import com.hmx.shield.features.permissions.presentation.screen.PermissionSetupScreen
import com.hmx.shield.features.securitycenter.presentation.screen.SecurityCenterScreen
import com.hmx.shield.features.settings.presentation.screen.SettingsScreen
import com.hmx.shield.features.onboarding.presentation.screen.SplashScreen
import com.hmx.shield.features.onboarding.presentation.screen.LockCreationScreen

// ─── Route Constants ──────────────────────────────────────────────────────────

object Routes {
    const val SPLASH           = "splash"
    const val WELCOME          = "welcome"
    const val ONBOARDING       = "onboarding"
    const val PERMISSION_SETUP = "permission_setup"
    const val LOCK_CREATION    = "lock_creation"
    const val DASHBOARD        = "dashboard"
    const val APP_LOCK_LIST    = "app_lock_list"
    const val APP_LOCK_CONFIG  = "app_lock_config/{packageName}"
    const val SECURITY_CENTER  = "security_center"
    const val SETTINGS         = "settings"
}

// ─── Navigation Graph ─────────────────────────────────────────────────────────

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // ── Onboarding Flow ─────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onSetupComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onSetupIncomplete = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.ONBOARDING) }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onContinue = { navController.navigate(Routes.PERMISSION_SETUP) }
            )
        }

        composable(Routes.PERMISSION_SETUP) {
            PermissionSetupScreen(
                onPermissionsReady = { navController.navigate(Routes.LOCK_CREATION) }
            )
        }

        composable(Routes.LOCK_CREATION) {
            LockCreationScreen(
                onLockCreated = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        // ── Main App ─────────────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToAppLock     = { navController.navigate(Routes.APP_LOCK_LIST) },
                onNavigateToSecurity    = { navController.navigate(Routes.SECURITY_CENTER) },
                onNavigateToSettings    = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.APP_LOCK_LIST) {
            AppLockListScreen(
                onNavigateBack    = { navController.popBackStack() },
                onConfigureApp    = { pkg ->
                    navController.navigate("app_lock_config/$pkg")
                }
            )
        }

        composable(Routes.APP_LOCK_CONFIG) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
            AppLockConfigScreen(
                packageName    = packageName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SECURITY_CENTER) {
            SecurityCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
