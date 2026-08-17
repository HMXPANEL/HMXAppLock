package com.hmx.shield

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import com.hmx.shield.core.Constants
import com.hmx.shield.core.ThemeController
import com.hmx.shield.core.security.SecurePreferences
import com.hmx.shield.crash.CrashContextHolder
import com.hmx.shield.crash.CrashReportRepository
import com.hmx.shield.ui.navigation.AppNav
import com.hmx.shield.ui.navigation.NavRoutes
import com.hmx.shield.ui.theme.HmxTheme
import com.hmx.shield.ui.theme.LocalThemeController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeController: ThemeController
    @Inject lateinit var securePreferences: SecurePreferences
    @Inject lateinit var crashRepository: CrashReportRepository
    @Inject lateinit var crashContextHolder: CrashContextHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Protect sensitive UI (dashboards, vault) from screenshots / recent-app leaks.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val startDestination = when {
            crashRepository.hasPending() -> NavRoutes.CRASH
            !securePreferences.getBoolean(Constants.PREF_SETUP_COMPLETE) -> NavRoutes.SPLASH
            else -> NavRoutes.DASHBOARD
        }
        crashContextHolder.setScreen("MainActivity:$startDestination")

        setContent {
            CompositionLocalProvider(LocalThemeController provides themeController) {
                HmxTheme {
                    AppNav(startDestination = startDestination)
                }
            }
        }
    }

    override fun onDestroy() {
        crashContextHolder.setScreen(null)
        super.onDestroy()
    }
}
