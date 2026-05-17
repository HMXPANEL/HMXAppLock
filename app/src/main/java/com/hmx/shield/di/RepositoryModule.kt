package com.hmx.shield.di

import com.hmx.shield.features.applock.data.repository.AppLockRepositoryImpl
import com.hmx.shield.features.applock.domain.repository.AppLockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds concrete repository implementations to their domain interfaces.
 * Using @Binds instead of @Provides avoids creating a wrapper object —
 * Hilt passes the impl reference directly, which is more efficient.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppLockRepository(
        impl: AppLockRepositoryImpl
    ): AppLockRepository
}
