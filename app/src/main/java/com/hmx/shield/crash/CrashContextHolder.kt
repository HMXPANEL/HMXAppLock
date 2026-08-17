package com.hmx.shield.crash

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight, volatile holder for the currently visible screen and a small amount
 * of safe app state. Updated from the UI layer. The crash handler reads this
 * without touching Android lifecycle or Compose. Never stores secrets here.
 */
@Singleton
class CrashContextHolder @Inject constructor() {
    @Volatile var currentScreen: String? = null
        private set

    @Volatile var protectionState: String? = null
        private set

    fun setScreen(name: String?) {
        currentScreen = name
    }

    fun setProtectionState(state: String?) {
        protectionState = state
    }
}
