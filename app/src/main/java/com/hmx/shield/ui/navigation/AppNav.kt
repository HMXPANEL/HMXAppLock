package com.hmx.shield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hmx.shield.features.applock.AppLockListScreen
import com.hmx.shield.features.authentication.CreateLockScreen
import com.hmx.shield.features.dashboard.DashboardScreen
import com.hmx.shield.features.intruder.IntruderLogsScreen
import com.hmx.shield.features.onboarding.OnboardingScreen
import com.hmx.shield.features.onboarding.PermissionSetupScreen
import com.hmx.shield.features.onboarding.SplashScreen
import com.hmx.shield.features.onboarding.WelcomeScreen
import com.hmx.shield.features.securitycenter.SecurityCenterScreen
import com.hmx.shield.features.settings.SettingsScreen
import com.hmx.shield.features.stealth.StealthScreen
import com.hmx.shield.features.themes.ThemeScreen
import com.hmx.shield.features.vault.VaultScreen
import com.hmx.shield.crash.ui.CrashReportScreen

@Composable
fun AppNav(startDestination: String) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination) {
        composable(NavRoutes.SPLASH) { SplashScreen(nav) }
        composable(NavRoutes.WELCOME) { WelcomeScreen(nav) }
        composable(NavRoutes.ONBOARDING) { OnboardingScreen(nav) }
        composable(NavRoutes.PERMISSIONS) { PermissionSetupScreen(nav) }
        composable(NavRoutes.CREATE_LOCK) { CreateLockScreen(nav) }
        composable(NavRoutes.DASHBOARD) { DashboardScreen(nav) }
        composable(NavRoutes.APP_LOCK) { AppLockListScreen(nav) }
        composable(NavRoutes.VAULT) { VaultScreen(nav) }
        composable(NavRoutes.INTRUDER) { IntruderLogsScreen(nav) }
        composable(NavRoutes.SECURITY) { SecurityCenterScreen(nav) }
        composable(NavRoutes.THEMES) { ThemeScreen(nav) }
        composable(NavRoutes.SETTINGS) { SettingsScreen(nav) }
        composable(NavRoutes.STEALTH) { StealthScreen(nav) }
        composable(NavRoutes.CRASH) { CrashReportScreen(nav) }
    }
}
