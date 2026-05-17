package com.hmx.shield.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hmx.shield.features.applock.data.dao.LockedAppsDao
import com.hmx.shield.features.applock.data.dao.UnlockSessionDao
import com.hmx.shield.features.applock.data.entities.LockedAppEntity
import com.hmx.shield.features.applock.data.entities.UnlockSessionEntity

/**
 * Single Room database for HMX Shield.
 *
 * Phase 2 tables: locked_apps, unlock_sessions
 *
 * Phase 3+ will add:
 *   VaultFileEntity, VaultFolderEntity, IntruderLogEntity,
 *   SecuritySettingsEntity, ThemeSettingsEntity, StealthConfigEntity
 *
 * Migration strategy: use proper Room migrations from v2 onward.
 * DatabaseModule uses fallbackToDestructiveMigration() in debug builds only.
 */
@Database(
    entities = [
        LockedAppEntity::class,
        UnlockSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lockedAppsDao(): LockedAppsDao
    abstract fun unlockSessionDao(): UnlockSessionDao

    companion object {
        const val DATABASE_NAME = "hmx_shield_db"
    }
}
