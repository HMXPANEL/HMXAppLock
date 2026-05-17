package com.hmx.shield.features.applock.data.dao

import androidx.room.*
import com.hmx.shield.features.applock.data.entities.UnlockSessionEntity

@Dao
interface UnlockSessionDao {

    /** Gets the raw session row for a package — ignores expiry */
    @Query("SELECT * FROM unlock_sessions WHERE packageName = :packageName LIMIT 1")
    suspend fun getSession(packageName: String): UnlockSessionEntity?

    /**
     * Core check: does this package have a valid (non-expired) session right now?
     *
     * Special cases:
     *   expiresAt = -1  → screen_off policy: always active until screen goes off
     *   expiresAt <= now → expired, returns false
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM unlock_sessions
            WHERE packageName = :packageName
            AND (expiresAt = -1 OR expiresAt > :now)
        )
    """)
    suspend fun hasActiveSession(packageName: String, now: Long = System.currentTimeMillis()): Boolean

    /** Insert or replace session for a package */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UnlockSessionEntity)

    /** Delete a specific package's session (used on explicit relock) */
    @Query("DELETE FROM unlock_sessions WHERE packageName = :packageName")
    suspend fun deleteSession(packageName: String)

    /**
     * Called on screen-off: clear all sessions whose policy is screen_off (expiresAt = -1).
     * These sessions should NOT survive a screen-off event.
     */
    @Query("DELETE FROM unlock_sessions WHERE expiresAt = -1")
    suspend fun clearScreenOffSessions()

    /** Garbage-collect genuinely expired sessions (non screen_off policy) */
    @Query("DELETE FROM unlock_sessions WHERE expiresAt != -1 AND expiresAt < :now")
    suspend fun clearExpiredSessions(now: Long = System.currentTimeMillis())

    /** Nuclear option: clear every session — used on reboot or PIN change */
    @Query("DELETE FROM unlock_sessions")
    suspend fun clearAllSessions()
}
