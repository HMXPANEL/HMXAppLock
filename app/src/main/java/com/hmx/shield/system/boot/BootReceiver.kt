package com.hmx.shield.system.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hmx.shield.system.overlay.OverlayService

/**
 * Restores protection after device reboot.
 *
 * Why needed:
 *   Android kills all services on reboot. Without this receiver, every locked app
 *   would be freely accessible after a restart until the user manually opens HMX Shield.
 *   This is the #1 most common app-lock bypass attack.
 *
 * What it does:
 *   1. Catches BOOT_COMPLETED (standard) and OEM-specific quick-boot actions
 *   2. Starts OverlayService as a foreground service
 *   3. OverlayService ACTION_RESTORE_MONITORING clears stale sessions (fresh start)
 *      and the AccessibilityService (if still enabled) resumes normal monitoring
 *
 * OEM notes:
 *   - Xiaomi/MIUI: requires Auto-Start to be enabled for this receiver to fire
 *   - Vivo: may delay BOOT_COMPLETED; the three-action filter covers alternative events
 *   - Oppo: similar to Vivo — both quickboot actions are included
 *
 * Permissions required in AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",      // HTC / some MediaTek OEMs
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {  // HTC legacy
                restoreProtection(context)
            }
        }
    }

    private fun restoreProtection(context: Context) {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_RESTORE_MONITORING
        }
        try {
            // startForegroundService required for Android 8.0+
            context.startForegroundService(intent)
        } catch (e: Exception) {
            // startForegroundService can fail on some OEMs during very early boot.
            // Android will retry BOOT_COMPLETED delivery — this is acceptable.
        }
    }
}
