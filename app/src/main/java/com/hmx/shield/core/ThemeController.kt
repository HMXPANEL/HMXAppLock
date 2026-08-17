package com.hmx.shield.core

import com.hmx.shield.core.model.AccentColor
import com.hmx.shield.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the active theme selection so Compose and the rest of the app share a
 * single source of truth. Updated from Settings; observed by [HmxTheme].
 */
@Singleton
class ThemeController @Inject constructor() {
    private val _state = MutableStateFlow(ThemeState(ThemeMode.DARK, AccentColor.PURPLE))
    val state: StateFlow<ThemeState> = _state.asStateFlow()

    fun update(mode: ThemeMode, accent: AccentColor) {
        _state.value = ThemeState(mode, accent)
    }

    fun setMode(mode: ThemeMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    fun setAccent(accent: AccentColor) {
        _state.value = _state.value.copy(accent = accent)
    }
}
