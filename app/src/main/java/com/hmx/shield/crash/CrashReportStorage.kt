package com.hmx.shield.crash

import android.content.Context
import com.hmx.shield.crash.model.CrashReport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists crash reports as individual JSON files inside app-private storage.
 * Designed to be called from the crash thread (no coroutines, no DB, no DI
 * initialization that could fail). All operations are defensive: if anything
 * throws, it is swallowed to avoid a recursive crash loop.
 */
@Singleton
class CrashReportStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val dir: File get() = File(context.filesDir, "crash").apply { mkdirs() }
    private val pendingFile: File get() = File(context.filesDir, "crash_pending.txt")
    private val maxReports = 25

    fun save(report: CrashReport) {
        runCatching {
            val file = File(dir, "report_${report.id}.json")
            file.writeText(json.encodeToString(CrashReport.serializer(), report))
            enforceRetention()
            pendingFile.writeText(report.id)
        }
    }

    fun loadAll(): List<CrashReport> = runCatching {
        dir.listFiles { f -> f.name.startsWith("report_") && f.name.endsWith(".json") }
            ?.mapNotNull { runCatching { json.decodeFromString(CrashReport.serializer(), it.readText()) }.getOrNull() }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }.getOrDefault(emptyList())

    fun load(id: String): CrashReport? = runCatching {
        File(dir, "report_$id.json").takeIf { it.exists() }?.let {
            json.decodeFromString(CrashReport.serializer(), it.readText())
        }
    }.getOrNull()

    fun delete(id: String) = runCatching { File(dir, "report_$id.json").delete() }
    fun clear() = runCatching {
        dir.listFiles()?.forEach { it.delete() }
        pendingFile.delete()
    }

    fun setPending(id: String) = runCatching { pendingFile.writeText(id) }
    fun getPendingId(): String? = runCatching { pendingFile.takeIf { it.exists() }?.readText()?.trim() }.getOrNull()
    fun clearPending() = runCatching { pendingFile.delete() }

    /** Keeps the most recent [maxReports] reports to bound local storage. */
    private fun enforceRetention() = runCatching {
        val files = dir.listFiles { f -> f.name.startsWith("report_") }
            ?.sortedByDescending { it.name } ?: return@runCatching
        if (files.size > maxReports) {
            files.drop(maxReports).forEach { it.delete() }
        }
    }
}
