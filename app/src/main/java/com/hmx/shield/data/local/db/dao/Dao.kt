package com.hmx.shield.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hmx.shield.data.local.db.entity.IntruderLogEntity
import com.hmx.shield.data.local.db.entity.LockedAppEntity
import com.hmx.shield.data.local.db.entity.SecuritySettingsEntity
import com.hmx.shield.data.local.db.entity.ThemeSettingsEntity
import com.hmx.shield.data.local.db.entity.UnlockSessionEntity
import com.hmx.shield.data.local.db.entity.VaultFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_apps")
    fun observeAll(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isEnabled = 1")
    suspend fun getEnabled(): List<LockedAppEntity>

    @Query("SELECT * FROM locked_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): LockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: LockedAppEntity)

    @Update
    suspend fun update(app: LockedAppEntity)

    @Delete
    suspend fun delete(app: LockedAppEntity)

    @Query("DELETE FROM locked_apps WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)
}

@Dao
interface UnlockSessionDao {
    @Query("SELECT * FROM unlock_sessions")
    suspend fun getAll(): List<UnlockSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: UnlockSessionEntity)

    @Query("DELETE FROM unlock_sessions")
    suspend fun clear()
}

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VaultFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: VaultFileEntity): Long

    @Delete
    suspend fun delete(file: VaultFileEntity)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface IntruderLogDao {
    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<IntruderLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: IntruderLogEntity)

    @Query("DELETE FROM intruder_logs")
    suspend fun clear()

    @Query("DELETE FROM intruder_logs WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface SecuritySettingsDao {
    @Query("SELECT * FROM security_settings WHERE id = 1")
    fun observe(): Flow<SecuritySettingsEntity?>

    @Query("SELECT * FROM security_settings WHERE id = 1")
    suspend fun get(): SecuritySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SecuritySettingsEntity)
}

@Dao
interface ThemeSettingsDao {
    @Query("SELECT * FROM theme_settings WHERE id = 1")
    fun observe(): Flow<ThemeSettingsEntity?>

    @Query("SELECT * FROM theme_settings WHERE id = 1")
    suspend fun get(): ThemeSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ThemeSettingsEntity)
}
