package com.hmx.shield.features.applock.domain.model

// ─── Relock Policy ───────────────────────────────────────────────────────────

/**
 * Controls when a successfully unlocked app re-locks.
 *
 * INSTANT    → Relock immediately when user leaves the app.
 *              Session expires at creation time. Highest security.
 * SCREEN_OFF → Relock when the screen turns off.
 *              Balanced: user can switch apps freely while screen is on.
 * TIMEOUT    → Relock after a configurable inactivity period.
 *              Most convenient; use for low-sensitivity apps.
 */
enum class RelockPolicy(val key: String) {
    INSTANT("instant"),
    SCREEN_OFF("screen_off"),
    TIMEOUT("timeout");

    companion object {
        fun fromKey(key: String): RelockPolicy =
            entries.firstOrNull { it.key == key } ?: INSTANT
    }
}

// ─── Lock Type ────────────────────────────────────────────────────────────────

enum class LockType(val key: String) {
    PIN("pin"),
    PATTERN("pattern"),
    PASSWORD("password"),
    FINGERPRINT("fingerprint");

    companion object {
        fun fromKey(key: String): LockType =
            entries.firstOrNull { it.key == key } ?: PIN
    }
}

// ─── Domain Model: LockedApp ──────────────────────────────────────────────────

data class LockedApp(
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val lockType: LockType,
    val relockPolicy: RelockPolicy,
    val relockTimeoutMs: Long = 300_000L,   // 5 min default — only used with TIMEOUT policy
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Domain Model: UnlockSession ─────────────────────────────────────────────

data class UnlockSession(
    val packageName: String,
    val unlockedAt: Long,
    val expiresAt: Long,          // -1L = screen_off sentinel
    val relockPolicy: RelockPolicy
) {
    /**
     * True if this session still grants access at [now].
     * Screen-off sessions are considered active until the
     * ScreenStateReceiver clears them; this method returns true for them.
     */
    fun isActive(now: Long = System.currentTimeMillis()): Boolean = when {
        expiresAt == -1L -> true       // screen_off: managed externally
        expiresAt == unlockedAt -> false // instant: immediately expired
        else -> now < expiresAt
    }
}
