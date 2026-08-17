package com.hmx.shield.core.security

import com.hmx.shield.core.Constants
import com.hmx.shield.core.model.RelockPolicy
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class AppLockInfo(
    val packageName: String,
    val appName: String,
    val relockPolicy: RelockPolicy
)

/**
 * In-memory cache of protected apps, mirrored from Room. The AccessibilityService
 * reads this synchronously (no DB access on the event thread). Updated by the
 * app-lock feature whenever the protected list changes and on app startup.
 */
@Singleton
class LockedAppCache @Inject constructor() {
    private val map = ConcurrentHashMap<String, AppLockInfo>()

    fun replaceAll(list: List<AppLockInfo>) {
        map.clear()
        list.forEach { map[it.packageName] = it }
    }

    fun add(info: AppLockInfo) { map[info.packageName] = info }
    fun remove(packageName: String) { map.remove(packageName) }
    fun isLocked(packageName: String): Boolean = map.containsKey(packageName)
    fun getInfo(packageName: String): AppLockInfo? = map[packageName]
    fun all(): List<AppLockInfo> = map.values.toList()
    fun snapshot(): List<AppLockInfo> = all()
}

/**
 * Tracks temporary unlock sessions so a user is not forced to re-enter the lock
 * on every activity transition inside the same protected app.
 */
@Singleton
class SessionManager @Inject constructor() {
    private data class Session(
        val packageName: String,
        val expiresAt: Long,
        val policy: RelockPolicy
    )

    private val sessions = ConcurrentHashMap<String, Session>()
    private val now: Long get() = System.currentTimeMillis()

    fun unlock(packageName: String, policy: RelockPolicy) {
        val expiresAt = when (policy) {
            RelockPolicy.TIMEOUT -> now + Constants.SESSION_TIMEOUT_MS
            RelockPolicy.SCREEN_OFF -> Long.MAX_VALUE
            RelockPolicy.INSTANT -> Long.MAX_VALUE
        }
        sessions[packageName] = Session(packageName, expiresAt, policy)
    }

    fun isUnlocked(packageName: String): Boolean {
        val session = sessions[packageName] ?: return false
        return if (session.policy == RelockPolicy.TIMEOUT) now < session.expiresAt else true
    }

    /**
     * Called by the monitor when the foreground app changes. INSTANT-policy
     * sessions are dropped the moment the protected app is no longer foreground.
     */
    fun onForegroundChanged(currentPackage: String) {
        sessions.entries.removeIf { (pkg, session) ->
            (session.policy == RelockPolicy.INSTANT && pkg != currentPackage)
        }
    }

    fun onScreenOff() {
        sessions.entries.removeIf { it.value.policy == RelockPolicy.SCREEN_OFF }
    }

    fun clearPackage(packageName: String) = sessions.remove(packageName)
    fun clearAll() = sessions.clear()
}
