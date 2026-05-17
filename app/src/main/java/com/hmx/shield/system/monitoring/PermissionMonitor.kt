package com.hmx.shield.system.monitoring

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.hmx.shield.core.security.LockStateManager
import com.hmx.shield.core.security.SecurityHealth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ─── Permission Status Data Class ────────────────────────────────────────────

data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val overlayGranted: Boolean,
    val usageAccessGranted: Boolean,
    val batteryOptimizationExempt: Boolean
) {
    /**
     * Derive overall security health from permission states.
     *
     * CRITICAL → core engine cannot function (overlay or accessibility missing)
     * WARNING  → protection degraded but partially working
     * HEALTHY  → all critical permissions active
     */
    val overallHealth: SecurityHealth
        get() = when {
            !accessibilityEnabled || !overlayGranted -> SecurityHealth.CRITICAL
            !usageAccessGranted || !batteryOptimizationExempt -> SecurityHealth.WARNING
            else -> SecurityHealth.HEALTHY
        }
}

// ─── PermissionMonitor ───────────────────────────────────────────────────────

@Singleton
class PermissionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lockStateManager: LockStateManager
) {
    private val _status = MutableStateFlow(checkAll())
    val status: StateFlow<PermissionStatus> = _status.asStateFlow()

    /**
     * Re-check all permissions and push the result to [status].
     * Should be called on:
     *   - App foreground
     *   - After returning from Settings
     *   - Service restart
     *   - Periodic background checks (every 30s via ServiceHealthMonitor)
     */
    fun refresh() {
        val current = checkAll()
        _status.value = current
        lockStateManager.updateSecurityHealth(current.overallHealth)
    }

    // ─── Individual Checks ───────────────────────────────────────────────────

    private fun checkAll(): PermissionStatus = PermissionStatus(
        accessibilityEnabled = isAccessibilityEnabled(),
        overlayGranted = isOverlayGranted(),
        usageAccessGranted = isUsageAccessGranted(),
        batteryOptimizationExempt = isBatteryOptimizationExempt()
    )

    fun isAccessibilityEnabled(): Boolean {
        // Check if our specific service appears in the enabled list
        val serviceName =
            "${context.packageName}/.system.accessibility.AppLockAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }

    fun isOverlayGranted(): Boolean = Settings.canDrawOverlays(context)

    fun isUsageAccessGranted(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun isBatteryOptimizationExempt(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
