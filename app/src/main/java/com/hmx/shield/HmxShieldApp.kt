package com.hmx.shield

import android.app.Application
import com.hmx.shield.core.ThemeController
import com.hmx.shield.core.model.AccentColor
import com.hmx.shield.core.model.ThemeMode
import com.hmx.shield.crash.CrashContextHolder
import com.hmx.shield.crash.CrashHandler
import com.hmx.shield.data.local.repository.LockedAppRepository
import com.hmx.shield.data.local.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HmxShieldApp : Application() {

    @Inject lateinit var crashHandler: CrashHandler
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var lockedAppRepository: LockedAppRepository
    @Inject lateinit var themeController: ThemeController
    @Inject lateinit var crashContextHolder: CrashContextHolder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Install the global crash handler as early as possible.
        crashHandler.install()
        crashContextHolder.setProtectionState("Starting")

        appScope.launch {
            runCatching {
                settingsRepository.ensureDefaults()
                lockedAppRepository.loadIntoCache()
                val theme = settingsRepository.observeTheme().first()
                themeController.update(
                    mode = ThemeMode.valueOf(theme.themeMode),
                    accent = runCatching { AccentColor.valueOf(theme.accentColor) }.getOrDefault(AccentColor.PURPLE)
                )
            }
        }
    }
}
