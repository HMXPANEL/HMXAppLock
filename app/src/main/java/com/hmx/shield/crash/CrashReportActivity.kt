package com.hmx.shield.crash

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.hmx.shield.core.ThemeController
import com.hmx.shield.crash.ui.CrashReportScreen
import com.hmx.shield.ui.theme.HmxTheme
import com.hmx.shield.ui.theme.LocalThemeController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Standalone entry point (launcher alias) that opens directly to the crash report list,
 * used when the app is launched after a previous crash.
 */
@AndroidEntryPoint
class CrashReportActivity : ComponentActivity() {
    @Inject lateinit var themeController: ThemeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            CompositionLocalProvider(LocalThemeController provides themeController) {
                HmxTheme {
                    CrashReportScreen(nav = rememberNavController())
                }
            }
        }
    }
}
