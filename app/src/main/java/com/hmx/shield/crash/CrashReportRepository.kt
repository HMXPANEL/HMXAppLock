package com.hmx.shield.crash

import com.hmx.shield.crash.model.CrashReport
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin access layer over [CrashReportStorage] for the UI. Keeps the storage's
 * defensive, synchronous contract while exposing simple helpers.
 */
@Singleton
class CrashReportRepository @Inject constructor(
    private val storage: CrashReportStorage
) {
    fun hasPending(): Boolean = storage.getPendingId() != null
    fun getPendingId(): String? = storage.getPendingId()
    fun getPending(): CrashReport? = storage.getPendingId()?.let { storage.load(it) }
    fun dismissPending() = storage.clearPending()

    fun loadAll(): List<CrashReport> = storage.loadAll()
    fun load(id: String): CrashReport? = storage.load(id)
    fun delete(id: String) = storage.delete(id)
    fun clearAll() = storage.clear()
}
