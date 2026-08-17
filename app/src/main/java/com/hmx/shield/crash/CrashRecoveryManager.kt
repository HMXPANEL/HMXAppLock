package com.hmx.shield.crash

import android.content.Context
import com.hmx.shield.core.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks recent crash frequency to prevent a crash/restart loop. If the app
 * crashes more than [Constants.CRASH_RETRY_LIMIT] times within [WINDOW_MS],
 * the app enters a safe mode on next launch (shows only the crash reporter and a
 * "reset" action) instead of re-running the full security engine.
 */
@Singleton
class CrashRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val file: File get() = File(context.filesDir, "crash_recovery.txt")
    private val windowMs = 10 * 60 * 1000L

    data class State(val count: Int, val lastTimestamp: Long)

    fun registerCrash(): Int {
        return runCatching {
            val (count, last) = read()
            val now = System.currentTimeMillis()
            val newCount = if (now - last > windowMs) 1 else count + 1
            file.writeText("$newCount|$now")
            newCount
        }.getOrDefault(1)
    }

    fun shouldEnterSafeMode(): Boolean = registerCrash() >= Constants.CRASH_RETRY_LIMIT

    fun reset() = runCatching { file.delete() }

    private fun read(): State = runCatching {
        val text = file.readText().trim()
        val parts = text.split("|")
        State(parts.getOrNull(0)?.toIntOrNull() ?: 0, parts.getOrNull(1)?.toLongOrNull() ?: 0L)
    }.getOrDefault(State(0, 0L))
}
