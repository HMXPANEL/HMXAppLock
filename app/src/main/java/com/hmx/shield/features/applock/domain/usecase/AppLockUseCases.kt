package com.hmx.shield.features.applock.domain.usecase

import com.hmx.shield.features.applock.domain.model.LockedApp
import com.hmx.shield.features.applock.domain.model.RelockPolicy
import com.hmx.shield.features.applock.domain.model.UnlockSession
import com.hmx.shield.features.applock.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ─── CheckProtectionUseCase ───────────────────────────────────────────────────

/**
 * Called on every AccessibilityEvent foreground-app change.
 *
 * Returns true  → app is protected AND no valid session exists → show overlay
 * Returns false → either not protected, or session is still active → let through
 *
 * Hot-path: must be IO-fast. Both underlying DAO queries are indexed.
 */
class CheckProtectionUseCase @Inject constructor(
    private val repository: AppLockRepository
) {
    suspend operator fun invoke(packageName: String): Boolean {
        val isLocked = repository.isAppLocked(packageName)
        if (!isLocked) return false
        val hasActiveSession = repository.hasActiveSession(packageName)
        return !hasActiveSession
    }
}

// ─── LockAppUseCase ───────────────────────────────────────────────────────────

/**
 * Adds a new app to the protected list.
 * Triggered by the user from the app-selection screen.
 */
class LockAppUseCase @Inject constructor(
    private val repository: AppLockRepository
) {
    suspend operator fun invoke(app: LockedApp) = repository.lockApp(app)
}

// ─── UnlockAppUseCase ─────────────────────────────────────────────────────────

/**
 * Called after the user successfully authenticates on the lock overlay.
 * Creates a session according to the app's configured relock policy.
 *
 * Policy mapping:
 *   INSTANT    → expiresAt = unlockedAt (expires immediately — forces re-auth next entry)
 *   SCREEN_OFF → expiresAt = -1L        (cleared by ScreenStateReceiver on ACTION_SCREEN_OFF)
 *   TIMEOUT    → expiresAt = now + timeoutMs
 */
class UnlockAppUseCase @Inject constructor(
    private val repository: AppLockRepository
) {
    suspend operator fun invoke(
        packageName: String,
        policy: RelockPolicy,
        timeoutMs: Long = 300_000L
    ) {
        val now = System.currentTimeMillis()
        val expiresAt = when (policy) {
            RelockPolicy.INSTANT    -> now            // already expired: next entry re-locks
            RelockPolicy.SCREEN_OFF -> -1L            // sentinel: cleared on screen off
            RelockPolicy.TIMEOUT    -> now + timeoutMs
        }
        repository.createSession(
            UnlockSession(
                packageName = packageName,
                unlockedAt = now,
                expiresAt = expiresAt,
                relockPolicy = policy
            )
        )
    }
}

// ─── GetLockedAppsUseCase ─────────────────────────────────────────────────────

/** Provides a live Flow of protected apps for the Dashboard and Settings screens */
class GetLockedAppsUseCase @Inject constructor(
    private val repository: AppLockRepository
) {
    operator fun invoke(): Flow<List<LockedApp>> = repository.observeLockedApps()
}
