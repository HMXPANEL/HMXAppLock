package com.hmx.shield.core.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central lock state manager. The single source of truth for what is currently locked
 * and whether the overlay should be visible.
 *
 * Communication bridge:
 *   AccessibilityService → calls triggerLock()  → OverlayService reacts
 *   OverlayService       → calls onAuthSuccess() → LockStateManager updates state
 *
 * Important: all StateFlows are collected on Main thread.
 * Mutations from IO threads use postValue-equivalent patterns via coroutine dispatchers.
 */
@Singleton
class LockStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ─── Public Streams ───────────────────────────────────────────────────────

    /** Current foreground package, updated on every AccessibilityEvent */
    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    /** Whether the lock overlay is currently showing */
    private val _overlayVisible = MutableStateFlow(false)
    val overlayVisible: StateFlow<Boolean> = _overlayVisible.asStateFlow()

    /** Package currently awaiting authentication on the overlay */
    private val _pendingLockPackage = MutableStateFlow<String?>(null)
    val pendingLockPackage: StateFlow<String?> = _pendingLockPackage.asStateFlow()

    /** Overall permission / protection health */
    private val _securityHealth = MutableStateFlow(SecurityHealth.HEALTHY)
    val securityHealth: StateFlow<SecurityHealth> = _securityHealth.asStateFlow()

    // ─── Debounce State ───────────────────────────────────────────────────────

    // Prevents rapid repeated overlay triggers during quick app-switching
    private var lastTriggerPackage: String? = null
    private var lastTriggerTime = 0L
    private val DEBOUNCE_MS = 250L

    // ─── Mutation Methods (called from services) ──────────────────────────────

    /** Called on every AccessibilityEvent foreground change */
    fun onForegroundAppChanged(packageName: String) {
        _foregroundPackage.value = packageName
    }

    /**
     * Instructs the system to show the lock overlay for [packageName].
     * Includes debounce protection to prevent rapid-switch bypasses.
     *
     * Returns true if the trigger was accepted (not debounced).
     */
    fun triggerLock(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        // Allow re-trigger if package changed, even within debounce window
        val samePackage = packageName == lastTriggerPackage
        if (samePackage && now - lastTriggerTime < DEBOUNCE_MS) return false

        lastTriggerPackage = packageName
        lastTriggerTime = now

        _pendingLockPackage.value = packageName
        _overlayVisible.value = true
        return true
    }

    /**
     * Called by OverlayService when authentication succeeds.
     * Clears overlay state for the authenticated package.
     */
    fun onAuthSuccess(packageName: String) {
        if (_pendingLockPackage.value == packageName) {
            _pendingLockPackage.value = null
            _overlayVisible.value = false
        }
    }

    /**
     * Called by OverlayService on auth failure.
     * Overlay stays visible — the UI handles showing error state.
     */
    fun onAuthFailed() {
        // Intentionally no state change: overlay remains, UI shows shake/error
    }

    /** Dismiss overlay without granting access (e.g. user backed out) */
    fun dismissOverlay() {
        _pendingLockPackage.value = null
        _overlayVisible.value = false
    }

    /** Updated by PermissionMonitor — surfaces to Dashboard security score */
    fun updateSecurityHealth(health: SecurityHealth) {
        _securityHealth.value = health
    }
}

// ─── SecurityHealth ───────────────────────────────────────────────────────────

enum class SecurityHealth {
    HEALTHY,   // All critical permissions active
    WARNING,   // Some permissions degraded but core lock still works
    CRITICAL   // Core lock engine cannot function (accessibility or overlay missing)
}
