package com.hmx.shield.core.model

enum class RelockPolicy {
    INSTANT,      // relock the moment the app leaves the foreground
    SCREEN_OFF,   // relock when the device screen turns off
    TIMEOUT       // relock after a fixed timeout window
}

enum class LockType {
    PIN,
    PATTERN,
    PASSWORD,
    BIOMETRIC    // biometric is a convenience unlock, never the sole stored secret
}

enum class ThemeMode {
    DARK,
    AMOLED,
    LIGHT,
    SYSTEM
}

enum class AccentColor(val hex: String) {
    PURPLE("#8B5CF6"),
    BLUE("#3B82F6"),
    CYAN("#06B6D4"),
    EMERALD("#10B981"),
    RED("#EF4444")
}

/**
 * Result of an authentication attempt. Biometric is always paired with a
 * knowledge factor (PIN/pattern/password) which remains the source of truth.
 */
sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val reason: String) : AuthResult
    data object LockedOut : AuthResult
}
