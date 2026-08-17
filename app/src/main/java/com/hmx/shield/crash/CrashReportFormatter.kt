package com.hmx.shield.crash

import com.hmx.shield.crash.model.CrashReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Produces a human-readable, copy/share-friendly crash log. In RELEASE builds the
 * trace is kept (it does not contain secrets) but verbose metadata beyond the
 * stack trace is trimmed. No credentials, PINs, tokens, or vault data are ever
 * included.
 */
object CrashReportFormatter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun format(report: CrashReport): String = buildString {
        appendLine("HMX SHIELD CRASH REPORT")
        appendLine("========================")
        appendLine("Timestamp:   ${dateFormat.format(Date(report.timestamp))}")
        appendLine("App Version: ${report.appVersion} (${report.versionCode})")
        appendLine("Build Type:  ${report.buildType}")
        appendLine("Android:     ${report.androidVersion} (API ${report.apiLevel})")
        appendLine("Device:      ${report.manufacturer} ${report.model}")
        appendLine("Process:     ${report.processName}")
        appendLine("Thread:      ${report.threadName}")
        appendLine()
        appendLine("Exception:   ${report.exceptionClass}")
        appendLine("Message:     ${report.message}")
        if (!report.screen.isNullOrBlank()) appendLine("Screen:      ${report.screen}")
        if (!report.contextInfo.isNullOrBlank()) appendLine("Context:     ${report.contextInfo}")
        if (!report.causedBy.isNullOrBlank()) {
            appendLine()
            appendLine("Caused By:")
            appendLine(report.causedBy)
        }
        appendLine()
        appendLine("Stack Trace:")
        appendLine(report.stackTrace)
    }
}
