package com.hmx.shield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hmx.shield.core.theme.HMXShieldTheme
import com.hmx.shield.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host. All navigation happens inside Compose NavHost.
 *
 * Responsibilities:
 *   - Install splash screen (Android 12+ native splash)
 *   - Enable edge-to-edge drawing
 *   - Set up the Compose content with theme + navigation
 *
 * Security: FLAG_SECURE is NOT set on MainActivity itself because that would
 * block the entire app UI. FLAG_SECURE is set only on the OverlayService's
 * WindowManager window so screenshots are blocked only during lock screen display.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install native splash screen — must be called before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HMXShieldTheme {
                AppNavGraph()
            }
        }
    }
}
