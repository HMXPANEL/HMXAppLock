package com.hmx.shield.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hmx.shield.data.local.db.dao.IntruderLogDao
import com.hmx.shield.data.local.db.dao.LockedAppDao
import com.hmx.shield.data.local.db.dao.SecuritySettingsDao
import com.hmx.shield.data.local.db.dao.ThemeSettingsDao
import com.hmx.shield.data.local.db.dao.UnlockSessionDao
import com.hmx.shield.data.local.db.dao.VaultFileDao
import com.hmx.shield.data.local.db.entity.IntruderLogEntity
import com.hmx.shield.data.local.db.entity.LockedAppEntity
import com.hmx.shield.data.local.db.entity.SecuritySettingsEntity
import com.hmx.shield.data.local.db.entity.ThemeSettingsEntity
import com.hmx.shield.data.local.db.entity.UnlockSessionEntity
import com.hmx.shield.data.local.db.entity.VaultFileEntity

@Database(
    entities = [
        LockedAppEntity::class,
        UnlockSessionEntity::class,
        VaultFileEntity::class,
        IntruderLogEntity::class,
        SecuritySettingsEntity::class,
        ThemeSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun unlockSessionDao(): UnlockSessionDao
    abstract fun vaultFileDao(): VaultFileDao
    abstract fun intruderLogDao(): IntruderLogDao
    abstract fun securitySettingsDao(): SecuritySettingsDao
    abstract fun themeSettingsDao(): ThemeSettingsDao
}
