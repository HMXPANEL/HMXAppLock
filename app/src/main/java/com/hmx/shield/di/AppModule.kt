package com.hmx.shield.di

import android.content.Context
import androidx.room.Room
import com.hmx.shield.core.Constants
import com.hmx.shield.data.local.db.AppDatabase
import com.hmx.shield.data.local.db.dao.IntruderLogDao
import com.hmx.shield.data.local.db.dao.LockedAppDao
import com.hmx.shield.data.local.db.dao.SecuritySettingsDao
import com.hmx.shield.data.local.db.dao.ThemeSettingsDao
import com.hmx.shield.data.local.db.dao.UnlockSessionDao
import com.hmx.shield.data.local.db.dao.VaultFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideLockedAppDao(db: AppDatabase) = db.lockedAppDao()
    @Provides fun provideUnlockSessionDao(db: AppDatabase) = db.unlockSessionDao()
    @Provides fun provideVaultFileDao(db: AppDatabase) = db.vaultFileDao()
    @Provides fun provideIntruderLogDao(db: AppDatabase) = db.intruderLogDao()
    @Provides fun provideSecuritySettingsDao(db: AppDatabase) = db.securitySettingsDao()
    @Provides fun provideThemeSettingsDao(db: AppDatabase) = db.themeSettingsDao()
}
