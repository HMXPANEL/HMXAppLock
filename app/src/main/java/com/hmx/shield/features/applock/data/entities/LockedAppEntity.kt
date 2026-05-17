package com.hmx.shield.features.applock.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores each protected app and its lock configuration.
 * Indexed by packageName (unique) for O(1) foreground-detection lookups.
 *
 * lockType     : "pin" | "pattern" | "password" | "fingerprint"
 * relockPolicy : "instant" | "screen_off" | "timeout"
 * relockTimeoutMs : used only when relockPolicy == "timeout"
 */
@Entity(
    tableName = "locked_apps",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class LockedAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val lockType: String = "pin",
    val relockPolicy: String = "instant",
    val relockTimeoutMs: Long = 300_000L,   // 5 min default for "timeout" policy
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
