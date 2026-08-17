package com.hmx.shield.data.local.repository

import android.content.Context
import android.net.Uri
import com.hmx.shield.core.Constants
import com.hmx.shield.core.model.RelockPolicy
import com.hmx.shield.core.security.CryptoManager
import com.hmx.shield.core.security.LockedAppCache
import com.hmx.shield.data.local.db.dao.IntruderLogDao
import com.hmx.shield.data.local.db.dao.LockedAppDao
import com.hmx.shield.data.local.db.dao.SecuritySettingsDao
import com.hmx.shield.data.local.db.dao.ThemeSettingsDao
import com.hmx.shield.data.local.db.dao.VaultFileDao
import com.hmx.shield.data.local.db.entity.IntruderLogEntity
import com.hmx.shield.data.local.db.entity.LockedAppEntity
import com.hmx.shield.data.local.db.entity.SecuritySettingsEntity
import com.hmx.shield.data.local.db.entity.ThemeSettingsEntity
import com.hmx.shield.data.local.db.entity.VaultFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockedAppRepository @Inject constructor(
    private val dao: LockedAppDao,
    private val cache: LockedAppCache
) {
    fun observeAll(): Flow<List<LockedAppEntity>> = dao.observeAll()

    suspend fun loadIntoCache() {
        val enabled = dao.getEnabled()
        cache.replaceAll(
            enabled.map {
                com.hmx.shield.core.security.AppLockInfo(
                    it.packageName, it.appName, RelockPolicy.valueOf(it.relockPolicy)
                )
            }
        )
    }

    suspend fun isLocked(packageName: String): Boolean = cache.isLocked(packageName)

    suspend fun get(packageName: String): LockedAppEntity? = dao.getByPackage(packageName)

    suspend fun upsert(entity: LockedAppEntity) {
        if (dao.getByPackage(entity.packageName) != null) dao.update(entity) else dao.insert(entity)
    }

    suspend fun add(packageName: String, appName: String, policy: RelockPolicy) {
        dao.insert(
            LockedAppEntity(
                packageName = packageName,
                appName = appName,
                lockType = "",
                relockPolicy = policy.name,
                isEnabled = true,
                createdAt = System.currentTimeMillis()
            )
        )
        cache.add(com.hmx.shield.core.security.AppLockInfo(packageName, appName, policy))
    }

    suspend fun updatePolicy(packageName: String, policy: RelockPolicy) {
        val existing = dao.getByPackage(packageName) ?: return
        dao.update(existing.copy(relockPolicy = policy.name, isEnabled = true))
        cache.add(com.hmx.shield.core.security.AppLockInfo(packageName, existing.appName, policy))
    }

    suspend fun remove(packageName: String) {
        dao.deleteByPackage(packageName)
        cache.remove(packageName)
    }

    suspend fun getEnabledPackages(): Set<String> = cache.all().map { it.packageName }.toSet()
}

@Singleton
class VaultRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val dao: VaultFileDao,
    private val crypto: CryptoManager
) {
    fun observeAll(): Flow<List<VaultFileEntity>> = dao.observeAll()

    suspend fun import(uri: Uri, fileType: String, originalName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                val encrypted = crypto.encrypt(bytes)
                val dir = File(context.filesDir, "vault").apply { mkdirs() }
                val fileName = UUID.randomUUID().toString() + ".enc"
                File(dir, fileName).writeText(encrypted)
                dao.insert(
                    VaultFileEntity(
                        encryptedPath = File(dir, fileName).absolutePath,
                        originalName = originalName,
                        fileType = fileType,
                        thumbnailPath = null,
                        folderId = null,
                        size = bytes.size.toLong(),
                        createdAt = System.currentTimeMillis()
                    )
                )
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /** Decrypt to a temporary cache file for secure viewing. Caller must delete after use. */
    suspend fun exportTemp(id: Int): File? = withContext(Dispatchers.IO) {
        val entity = dao.observeAll().firstOrNull()?.firstOrNull { it.id == id } ?: return@withContext null
        try {
            val cipher = File(entity.encryptedPath).readText()
            val bytes = crypto.decrypt(cipher)
            val tmpDir = File(context.cacheDir, "vault_view").apply { mkdirs() }
            val out = File(tmpDir, UUID.randomUUID().toString())
            out.writeBytes(bytes)
            out
        } catch (_: Exception) {
            null
        }
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val entity = dao.observeAll().firstOrNull()?.firstOrNull { it.id == id }
        if (entity != null) {
            dao.deleteById(id)
            runCatching { File(entity.encryptedPath).delete() }
        }
    }
}

@Singleton
class IntruderRepository @Inject constructor(
    private val dao: IntruderLogDao
) {
    fun observeAll(): Flow<List<IntruderLogEntity>> = dao.observeAll()

    suspend fun record(imagePath: String, packageName: String, attempts: Int, batteryPercent: Int) {
        dao.insert(
            IntruderLogEntity(
                imagePath = imagePath,
                packageName = packageName,
                failedAttempts = attempts,
                batteryPercent = batteryPercent,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clear() = dao.clear()

    suspend fun delete(id: Int) = dao.deleteById(id)
}

@Singleton
class SettingsRepository @Inject constructor(
    private val securityDao: SecuritySettingsDao,
    private val themeDao: ThemeSettingsDao
) {
    private val defaultSecurity = SecuritySettingsEntity(
        id = 1, stealthMode = false, intruderDetectionEnabled = false,
        screenshotProtectionEnabled = true, biometricEnabled = false
    )
    private val defaultTheme = ThemeSettingsEntity(
        id = 1, themeMode = "DARK", accentColor = "PURPLE", blurEnabled = true, animationEnabled = true
    )

    fun observeSecurity(): Flow<SecuritySettingsEntity> =
        kotlinx.coroutines.flow.map(securityDao.observe()) { it ?: defaultSecurity }

    fun observeTheme(): Flow<ThemeSettingsEntity> =
        kotlinx.coroutines.flow.map(themeDao.observe()) { it ?: defaultTheme }

    suspend fun ensureDefaults() {
        if (securityDao.get() == null) securityDao.upsert(defaultSecurity)
        if (themeDao.get() == null) themeDao.upsert(defaultTheme)
    }

    suspend fun setIntruderEnabled(enabled: Boolean) {
        val cur = securityDao.get() ?: defaultSecurity
        securityDao.upsert(cur.copy(intruderDetectionEnabled = enabled))
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        val cur = securityDao.get() ?: defaultSecurity
        securityDao.upsert(cur.copy(biometricEnabled = enabled))
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        val cur = securityDao.get() ?: defaultSecurity
        securityDao.upsert(cur.copy(screenshotProtectionEnabled = enabled))
    }

    suspend fun setStealthMode(enabled: Boolean) {
        val cur = securityDao.get() ?: defaultSecurity
        securityDao.upsert(cur.copy(stealthMode = enabled))
    }

    suspend fun setThemeMode(mode: String) {
        val cur = themeDao.get() ?: defaultTheme
        themeDao.upsert(cur.copy(themeMode = mode))
    }

    suspend fun setAccent(color: String) {
        val cur = themeDao.get() ?: defaultTheme
        themeDao.upsert(cur.copy(accentColor = color))
    }
}
