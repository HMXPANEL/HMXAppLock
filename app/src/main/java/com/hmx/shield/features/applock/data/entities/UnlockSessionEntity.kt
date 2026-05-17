package com.hmx.shield.features.applock.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks active unlock sessions so the user isn't re-prompted every time
 * they switch away and back during a valid session window.
 *
 * expiresAt == -1L  → "screen_off" policy: session lives until screen turns off
 * expiresAt == now  → "instant" policy:    session expires immediately (forces re-auth each entry)
 * expiresAt == future → "timeout" policy:  session valid until timestamp
 */
@Entity(
    tableName = "unlock_sessions",
    indices = [Index(value = ["packageName"])]
)
data class UnlockSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val unlockedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long
)
