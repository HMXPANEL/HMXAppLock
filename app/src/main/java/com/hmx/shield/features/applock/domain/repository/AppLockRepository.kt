package com.hmx.shield.features.applock.domain.repository

import com.hmx.shield.features.applock.domain.model.LockedApp
import com.hmx.shield.features.applock.domain.model.UnlockSession
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {

    // ─── Locked Apps ──────────────────────────────────────────────────────────

    /** Live stream of all enabled locked apps — for Dashboard/Settings UI */
    fun observeLockedApps(): Flow<List<LockedApp>>

    /** Full list including disabled apps — for management screens */
    fun observeAllLockedApps(): Flow<List<LockedApp>>

    /** Single lookup — used after lock trigger to get relock policy */
    suspend fun getLockedApp(packageName: String): LockedApp?

    /**
     * Fast boolean called on every foreground app change.
     * Must be as fast as possible — runs on IO dispatcher inside accessibility service.
     */
    suspend fun isAppLocked(packageName: String): Boolean

    /** Bulk lookup — used to pre-populate in-memory protected package set */
    suspend fun getLockedPackageNames(): List<String>

    /** Add or update a protected app */
    suspend fun lockApp(app: LockedApp)

    /** Remove a protected app completely */
    suspend fun unlockApp(packageName: String)

    /** Toggle protection without removing the entry */
    suspend fun setAppEnabled(packageName: String, enabled: Boolean)

    // ─── Sessions ─────────────────────────────────────────────────────────────

    /** Create a new unlock session — called after successful authentication */
    suspend fun createSession(session: UnlockSession)

    /**
     * True if package has a valid (non-expired) session.
     * This + isAppLocked() determine whether to show the overlay.
     */
    suspend fun hasActiveSession(packageName: String): Boolean

    /** Invalidate a specific session (explicit relock) */
    suspend fun clearSession(packageName: String)

    /** Remove all screen_off policy sessions — called on ACTION_SCREEN_OFF */
    suspend fun clearScreenOffSessions()

    /** Garbage-collect timed-out sessions — called periodically */
    suspend fun clearExpiredSessions()

    /** Wipe all sessions — called on device reboot or PIN change */
    suspend fun clearAllSessions()
}
