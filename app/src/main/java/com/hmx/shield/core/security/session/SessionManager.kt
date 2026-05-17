package com.hmx.shield.core.security.session

import com.hmx.shield.features.applock.domain.model.RelockPolicy
import com.hmx.shield.features.applock.domain.repository.AppLockRepository
import com.hmx.shield.features.applock.domain.usecase.UnlockAppUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level orchestrator for unlock sessions.
 *
 * Used by:
 *   OverlayService      → grantSession() after successful auth
 *   ScreenStateReceiver → onScreenOff() to clear screen_off policy sessions
 *   BootReceiver        → clearAll() on device restart
 */
@Singleton
class SessionManager @Inject constructor(
    private val unlockAppUseCase: UnlockAppUseCase,
    private val repository: AppLockRepository
) {
    // Dedicated IO scope: session operations are DB-bound, never block Main
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Grant access to [packageName] by creating a session under [policy].
     * Called by OverlayService immediately after authentication succeeds.
     *
     * [timeoutMs] is only used when policy == TIMEOUT.
     */
    fun grantSession(
        packageName: String,
        policy: RelockPolicy,
        timeoutMs: Long = 300_000L
    ) {
        scope.launch {
            unlockAppUseCase(packageName, policy, timeoutMs)
        }
    }

    /**
     * Called when screen turns off (ACTION_SCREEN_OFF).
     * Clears all SCREEN_OFF policy sessions and expired timeout sessions.
     */
    fun onScreenOff() {
        scope.launch {
            repository.clearScreenOffSessions()
            repository.clearExpiredSessions()
        }
    }

    /**
     * Explicitly revoke access to a single app (e.g. manual relock from dashboard).
     */
    fun revokeSession(packageName: String) {
        scope.launch { repository.clearSession(packageName) }
    }

    /**
     * Wipe all active sessions.
     * Called on:  device reboot, master PIN change, stealth mode toggle
     */
    fun clearAllSessions() {
        scope.launch { repository.clearAllSessions() }
    }

    /**
     * Periodic housekeeping — purge timed-out sessions.
     * Called on screen on and after service restart.
     */
    fun cleanupExpired() {
        scope.launch { repository.clearExpiredSessions() }
    }

    /** Suspend version for callers that need to await the result */
    suspend fun hasActiveSession(packageName: String): Boolean =
        repository.hasActiveSession(packageName)
}
