package com.hmx.shield.features.applock.data.mapper

import com.hmx.shield.features.applock.data.entities.LockedAppEntity
import com.hmx.shield.features.applock.data.entities.UnlockSessionEntity
import com.hmx.shield.features.applock.domain.model.LockedApp
import com.hmx.shield.features.applock.domain.model.LockType
import com.hmx.shield.features.applock.domain.model.RelockPolicy
import com.hmx.shield.features.applock.domain.model.UnlockSession

// ─── LockedApp Mappers ────────────────────────────────────────────────────────

fun LockedAppEntity.toDomain(): LockedApp = LockedApp(
    id = id,
    packageName = packageName,
    appName = appName,
    lockType = LockType.fromKey(lockType),
    relockPolicy = RelockPolicy.fromKey(relockPolicy),
    relockTimeoutMs = relockTimeoutMs,
    isEnabled = isEnabled,
    createdAt = createdAt
)

fun LockedApp.toEntity(): LockedAppEntity = LockedAppEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    lockType = lockType.key,
    relockPolicy = relockPolicy.key,
    relockTimeoutMs = relockTimeoutMs,
    isEnabled = isEnabled,
    createdAt = createdAt
)

// ─── UnlockSession Mappers ────────────────────────────────────────────────────

fun UnlockSessionEntity.toDomain(policy: RelockPolicy): UnlockSession = UnlockSession(
    packageName = packageName,
    unlockedAt = unlockedAt,
    expiresAt = expiresAt,
    relockPolicy = policy
)

fun UnlockSession.toEntity(): UnlockSessionEntity = UnlockSessionEntity(
    packageName = packageName,
    unlockedAt = unlockedAt,
    expiresAt = expiresAt
)
