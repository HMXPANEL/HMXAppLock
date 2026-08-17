package com.hmx.shield.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locked_apps",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class LockedAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val lockType: String,
    val relockPolicy: String,
    val isEnabled: Boolean,
    val createdAt: Long
)

@Entity(tableName = "unlock_sessions")
data class UnlockSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val unlockedAt: Long,
    val expiresAt: Long
)

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val encryptedPath: String,
    val originalName: String,
    val fileType: String,
    val thumbnailPath: String?,
    val folderId: Int?,
    val size: Long,
    val createdAt: Long
)

@Entity(tableName = "intruder_logs")
data class IntruderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val packageName: String,
    val failedAttempts: Int,
    val batteryPercent: Int,
    val timestamp: Long
)

@Entity(tableName = "security_settings")
data class SecuritySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val stealthMode: Boolean,
    val intruderDetectionEnabled: Boolean,
    val screenshotProtectionEnabled: Boolean,
    val biometricEnabled: Boolean
)

@Entity(tableName = "theme_settings")
data class ThemeSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String,
    val accentColor: String,
    val blurEnabled: Boolean,
    val animationEnabled: Boolean
)

@Entity(tableName = "permission_states")
data class PermissionStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val permissionName: String,
    val granted: Boolean,
    val lastChecked: Long
)
