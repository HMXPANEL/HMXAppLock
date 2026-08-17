package com.hmx.shield.crash

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.hmx.shield.BuildConfig
import com.hmx.shield.crash.model.CrashReport
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global uncaught-exception handler. Captures a concise, secret-free diagnostic
 * snapshot and persists it synchronously to app-private storage BEFORE the
 * process is allowed to terminate. It then delegates to the previously installed
 * handler so Android performs its normal process-teardown.
 *
 * It must never:
 *  - use coroutines / Compose / Room
 *  - access the network
 *  - allocate large objects
 *  - re-throw (which would destroy the captured report)
 */
@Singleton
class CrashHandler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val storage: CrashReportStorage,
    private val recoveryManager: CrashRecoveryManager,
    private val contextHolder: CrashContextHolder
) : Thread.UncaughtExceptionHandler {

    private val previousHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        runCatching { persist(thread, ex) }
        // Allow Android's normal crash termination to proceed.
        previousHandler?.uncaughtException(thread, ex)
            ?: run { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(1) }
    }

    private fun persist(thread: Thread, ex: Throwable) {
        val report = buildReport(thread, ex)
        storage.save(report)
        storage.setPending(report.id)
        recoveryManager.registerCrash()
    }

    private fun buildReport(thread: Thread, ex: Throwable): CrashReport {
        val pm = appContext.packageManager
        val pkgInfo = runCatching { pm.getPackageInfo(appContext.packageName, 0) }.getOrNull()
        val versionCode = pkgInfo?.let {
            if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
        } ?: 0L

        return CrashReport(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            appVersion = pkgInfo?.versionName ?: "unknown",
            versionCode = versionCode,
            buildType = BuildConfig.BUILD_TYPE,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            processName = currentProcessName(),
            threadName = thread.name,
            exceptionClass = ex.javaClass.name,
            message = ex.message ?: "",
            stackTrace = stackTraceToString(ex),
            causedBy = ex.cause?.let { stackTraceToString(it) },
            screen = contextHolder.currentScreen,
            contextInfo = contextHolder.protectionState
        )
    }

    private fun stackTraceToString(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        // Render suppressed exceptions for completeness.
        t.suppressed.forEach { s -> s.printStackTrace(pw) }
        pw.flush()
        return sw.toString().take(8000)
    }

    private fun currentProcessName(): String = runCatching {
        val pid = android.os.Process.myPid()
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
    }.getOrNull() ?: appContext.packageName
}
