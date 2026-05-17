package com.hmx.shield.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.hmx.shield.core.security.LockStateManager
import com.hmx.shield.features.applock.domain.usecase.CheckProtectionUseCase
import com.hmx.shield.system.monitoring.ServiceHealthMonitor
import com.hmx.shield.system.overlay.OverlayService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Primary foreground-app detection engine.
 *
 * Listens for TYPE_WINDOW_STATE_CHANGED events — fired every time a new
 * Activity window comes to the foreground. On each event:
 *   1. Filter noise (system UI, our own app, rapid duplicates)
 *   2. Check if the package is protected (CheckProtectionUseCase)
 *   3. If yes → triggerLock → start OverlayService
 *
 * Performance contract:
 *   - onAccessibilityEvent() must return in <2ms — all async work is launched
 *     into a coroutine scope immediately, never blocking the event callback.
 *   - Package checks use an indexed Room query; typical IO time is <5ms.
 *
 * OEM notes:
 *   - Some OEMs (Xiaomi/MIUI, Vivo) fire extra WINDOW_STATE_CHANGED events
 *     during animations. The debounce + package-equality guard handles this.
 *   - Huawei/EMUI may pause the service under battery saver; ServiceHealthMonitor
 *     detects missed heartbeats and triggers recovery guidance.
 */
@AndroidEntryPoint
class AppLockAccessibilityService : AccessibilityService() {

    @Inject lateinit var lockStateManager: LockStateManager
    @Inject lateinit var checkProtectionUseCase: CheckProtectionUseCase
    @Inject lateinit var serviceHealthMonitor: ServiceHealthMonitor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var heartbeatJob: Job? = null

    // ─── Debounce State ───────────────────────────────────────────────────────
    private var lastProcessedPackage: String? = null
    private var lastEventTime = 0L
    private val DEBOUNCE_MS = 250L   // Ignore repeated events for same package within 250ms

    // ─── Package Ignore List ──────────────────────────────────────────────────
    // System launchers and UI components that should never trigger a lock check.
    // OEM-specific launchers are included to prevent false triggers.
    private val IGNORED_PACKAGES = setOf(
        applicationContext.packageName,       // Our own app
        "com.android.systemui",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",                      // Xiaomi
        "com.sec.android.app.launcher",       // Samsung
        "com.huawei.android.launcher",        // Huawei
        "com.oppo.launcher",                  // Oppo
        "com.vivo.launcher",                  // Vivo
        "com.oneplus.launcher"                // OnePlus
    )

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure to receive only window-state changes (most efficient filter)
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100  // Debounce at Android level too
        }

        startHeartbeat()
    }

    // ─── Core Event Handler ───────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Only care about foreground window changes
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString()?.trim() ?: return

        // Fast path: ignore system/launcher packages
        if (packageName in IGNORED_PACKAGES) return

        val now = System.currentTimeMillis()

        // Debounce: skip rapid repeated events for the same package
        // (protects against Xiaomi/Vivo animation event bursts)
        if (packageName == lastProcessedPackage && now - lastEventTime < DEBOUNCE_MS) return

        lastProcessedPackage = packageName
        lastEventTime = now

        // Notify state manager immediately (fast, no IO)
        lockStateManager.onForegroundAppChanged(packageName)

        // Async: check if package needs protection (Room IO query)
        serviceScope.launch(Dispatchers.IO) {
            val needsLock = checkProtectionUseCase(packageName)
            if (needsLock) {
                // Switch to Main to mutate state + start service
                launch(Dispatchers.Main) {
                    val accepted = lockStateManager.triggerLock(packageName)
                    if (accepted) {
                        startOverlayService(packageName)
                    }
                }
            }
        }
    }

    // ─── Overlay Trigger ──────────────────────────────────────────────────────

    private fun startOverlayService(packageName: String) {
        val intent = Intent(applicationContext, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_OVERLAY
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
        }
        try {
            applicationContext.startForegroundService(intent)
        } catch (e: Exception) {
            // Service start can fail in very rare edge cases (app being killed)
            // LockStateManager.dismissOverlay() keeps state consistent
            lockStateManager.dismissOverlay()
        }
    }

    // ─── Heartbeat ────────────────────────────────────────────────────────────

    /**
     * Periodically notifies ServiceHealthMonitor that this service is alive.
     * If this service is killed by an OEM, the heartbeat stops and
     * ServiceHealthMonitor triggers recovery guidance on the Dashboard.
     */
    private fun startHeartbeat() {
        heartbeatJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                serviceHealthMonitor.recordHeartbeat()
                delay(ServiceHealthMonitor.HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    // ─── Interruption & Teardown ──────────────────────────────────────────────

    override fun onInterrupt() {
        // Service interrupted — PermissionMonitor detects this via isAccessibilityEnabled()
        // and updates SecurityHealth to CRITICAL on next refresh cycle
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatJob?.cancel()
        serviceScope.cancel()
    }
}
