package com.hmx.shield.features.applock.data.repository

import com.hmx.shield.features.applock.data.dao.LockedAppsDao
import com.hmx.shield.features.applock.data.dao.UnlockSessionDao
import com.hmx.shield.features.applock.data.mapper.toDomain
import com.hmx.shield.features.applock.data.mapper.toEntity
import com.hmx.shield.features.applock.domain.model.LockedApp
import com.hmx.shield.features.applock.domain.model.UnlockSession
import com.hmx.shield.features.applock.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockRepositoryImpl @Inject constructor(
    private val lockedAppsDao: LockedAppsDao,
    private val unlockSessionDao: UnlockSessionDao
) : AppLockRepository {

    // ─── Locked Apps ──────────────────────────────────────────────────────────

    override fun observeLockedApps(): Flow<List<LockedApp>> =
        lockedAppsDao.observeLockedApps()
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeAllLockedApps(): Flow<List<LockedApp>> =
        lockedAppsDao.observeAllLockedApps()
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getLockedApp(packageName: String): LockedApp? =
        lockedAppsDao.getLockedApp(packageName)?.toDomain()

    override suspend fun isAppLocked(packageName: String): Boolean =
        lockedAppsDao.isAppLocked(packageName)

    override suspend fun getLockedPackageNames(): List<String> =
        lockedAppsDao.getLockedPackageNames()

    override suspend fun lockApp(app: LockedApp) =
        lockedAppsDao.insertApp(app.toEntity())

    override suspend fun unlockApp(packageName: String) =
        lockedAppsDao.deleteApp(packageName)

    override suspend fun setAppEnabled(packageName: String, enabled: Boolean) =
        lockedAppsDao.setEnabled(packageName, enabled)

    // ─── Sessions ─────────────────────────────────────────────────────────────

    override suspend fun createSession(session: UnlockSession) =
        unlockSessionDao.insertSession(session.toEntity())

    override suspend fun hasActiveSession(packageName: String): Boolean =
        unlockSessionDao.hasActiveSession(packageName)

    override suspend fun clearSession(packageName: String) =
        unlockSessionDao.deleteSession(packageName)

    override suspend fun clearScreenOffSessions() =
        unlockSessionDao.clearScreenOffSessions()

    override suspend fun clearExpiredSessions() =
        unlockSessionDao.clearExpiredSessions()

    override suspend fun clearAllSessions() =
        unlockSessionDao.clearAllSessions()
}
