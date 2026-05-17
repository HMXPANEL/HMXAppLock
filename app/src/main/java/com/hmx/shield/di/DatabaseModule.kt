// ─────────────────────────────────────────────────────────────────────────────
// DatabaseModule.kt
// ─────────────────────────────────────────────────────────────────────────────
package com.hmx.shield.di

import android.content.Context
import androidx.room.Room
import com.hmx.shield.core.database.AppDatabase
import com.hmx.shield.features.applock.data.dao.LockedAppsDao
import com.hmx.shield.features.applock.data.dao.UnlockSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        // Phase 2: use fallbackToDestructiveMigration for development speed.
        // Phase 3: replace with real addMigrations() before Play Store release.
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideLockedAppsDao(db: AppDatabase): LockedAppsDao =
        db.lockedAppsDao()

    @Provides
    fun provideUnlockSessionDao(db: AppDatabase): UnlockSessionDao =
        db.unlockSessionDao()
}
