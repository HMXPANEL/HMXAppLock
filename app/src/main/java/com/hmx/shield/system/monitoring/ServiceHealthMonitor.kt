package com.hmx.shield.system.monitoring

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks service liveness via periodic heartbeats.
 *
 * AccessibilityService and OverlayService call [recordHeartbeat] every
 * [HEARTBEAT_INTERVAL_MS] milliseconds. If three consecutive heartbeats are
 * missed, [onServiceUnhealthy] fires — indicating an OEM killed the service.
 *
 * Why 3 missed heartbeats (not 1)?
 * OEMs sometimes pause services briefly during screen sleep without truly
 * killing them. Three missed beats (90s by default) signals a genuine kill.
 */
@Singleton
class ServiceHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionMonitor: PermissionMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchdogJob: Job? = null

    @Volatile private var lastHeartbeatMs = 0L

    companion object {
        const val HEARTBEAT_INTERVAL_MS = 30_000L   // Service beats every 30s
        const val HEARTBEAT_TIMEOUT_MS  = 90_000L   // 3 missed = dead
    }

    fun startWatchdog() {
        if (watchdogJob?.isActive == true) return
        watchdogJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                checkHealth()
            }
        }
    }

    /** Called by AccessibilityService and OverlayService periodically */
    fun recordHeartbeat() {
        lastHeartbeatMs = System.currentTimeMillis()
    }

    private fun checkHealth() {
        // First heartbeat hasn't happened yet — service just started
        if (lastHeartbeatMs == 0L) return

        val elapsed = System.currentTimeMillis() - lastHeartbeatMs
        if (elapsed > HEARTBEAT_TIMEOUT_MS) {
            onServiceUnhealthy()
        }
    }

    /**
     * Called when service appears dead.
     * Refreshes permission state (may show CRITICAL health on dashboard).
     * Auto-restart is handled via BOOT_COMPLETED / START_STICKY — no manual
     * restart attempt here to avoid restart loops that OEMs detect and suppress.
     */
    private fun onServiceUnhealthy() {
        permissionMonitor.refresh()
        lastHeartbeatMs = 0L // Reset so we don't spam this callback
    }

    fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }
}
