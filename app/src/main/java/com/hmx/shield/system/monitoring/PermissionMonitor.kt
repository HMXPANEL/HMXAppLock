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

// ─── Permission Status ────────────────────────────────────────────────────────

data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val overlayGranted: Boolean,
    val usageAccessGranted: Boolean,
    val batteryOptimizationExempt: Boolean
) {
    val overallHealth: SecurityHealth
        get() = when {
            !accessibilityEnabled || !overlayGranted -> SecurityHealth.CRITICAL
            !usageAccessGranted || !batteryOptimizationExempt -> SecurityHealth.WARNING
            else -> SecurityHealth.HEALTHY
        }
}

// ─── PermissionMonitor ────────────────────────────────────────────────────────

@Singleton
class PermissionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lockStateManager: LockStateManager
) {
    private val _status = MutableStateFlow(checkAll())
    val status: StateFlow<PermissionStatus> = _status.asStateFlow()

    fun refresh() {
        val current = checkAll()
        _status.value = current
        lockStateManager.updateSecurityHealth(current.overallHealth)
    }

    // ─── Individual Checks ────────────────────────────────────────────────────

    private fun checkAll(): PermissionStatus = PermissionStatus(
        accessibilityEnabled      = isAccessibilityEnabled(),
        overlayGranted            = isOverlayGranted(),
        usageAccessGranted        = isUsageAccessGranted(),
        batteryOptimizationExempt = isBatteryOptimizationExempt()
    )

    fun isAccessibilityEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            // The service class full name never changes regardless of build type
            val serviceClass =
                "com.hmx.shield.system.accessibility.AppLockAccessibilityService"

            // The package name DOES change between debug (.debug suffix) and release
            // So we check if the service class appears anywhere in the enabled list
            // This handles: com.hmx.shield/..., com.hmx.shield.debug/..., etc.
            enabledServices
                .split(":")
                .any { entry -> entry.contains(serviceClass) }

        } catch (e: Exception) {
            false
        }
    }

    fun isOverlayGranted(): Boolean =
        Settings.canDrawOverlays(context)

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
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (e: Exception) {
            false
        }
    }
}
