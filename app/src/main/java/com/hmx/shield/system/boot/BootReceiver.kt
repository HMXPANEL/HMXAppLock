package com.hmx.shield.system.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Kept intentionally lightweight. When the device reboots and the Accessibility
 * protection engine is re-enabled by the system, the app process restarts and
 * [com.hmx.shield.HmxShieldApp] reloads the protected-app cache and settings.
 * No heavy work is done here so a failure can never prevent boot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: bootstrap happens in HmxShieldApp.onCreate on process start.
    }
}
