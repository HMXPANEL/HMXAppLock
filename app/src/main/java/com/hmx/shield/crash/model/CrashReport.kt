package com.hmx.shield.crash.model

import kotlinx.serialization.Serializable

@Serializable
data class CrashReport(
    val id: String,
    val timestamp: Long,
    val appVersion: String,
    val versionCode: Long,
    val buildType: String,
    val androidVersion: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val processName: String,
    val threadName: String,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
    val causedBy: String?,
    val screen: String?,
    val contextInfo: String?
) {
    companion object {
        const val REDACTED = "[redacted]"
    }
}
