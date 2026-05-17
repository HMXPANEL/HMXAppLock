package com.hmx.shield.features.applock.data.dao

import androidx.room.*
import com.hmx.shield.features.applock.data.entities.LockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppsDao {

    /** Live list of enabled locked apps — observed by dashboard and ViewModel */
    @Query("SELECT * FROM locked_apps WHERE isEnabled = 1")
    fun observeLockedApps(): Flow<List<LockedAppEntity>>

    /** Full list including disabled entries — for settings/management screen */
    @Query("SELECT * FROM locked_apps")
    fun observeAllLockedApps(): Flow<List<LockedAppEntity>>

    /**
     * Fast package lookup used by AccessibilityService hot-path.
     * Returns null if package is not locked.
     */
    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getLockedApp(packageName: String): LockedAppEntity?

    /**
     * Ultra-fast boolean check called on every AccessibilityEvent.
     * Critical: must return in <5ms to avoid blocking the event thread.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE packageName = :packageName AND isEnabled = 1)")
    suspend fun isAppLocked(packageName: String): Boolean

    /** Returns all protected package names — used for preloading the in-memory cache */
    @Query("SELECT packageName FROM locked_apps WHERE isEnabled = 1")
    suspend fun getLockedPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: LockedAppEntity)

    @Update
    suspend fun updateApp(app: LockedAppEntity)

    @Query("UPDATE locked_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Delete
    suspend fun delete(app: LockedAppEntity)
}
