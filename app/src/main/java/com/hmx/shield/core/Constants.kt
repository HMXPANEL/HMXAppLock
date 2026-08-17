package com.hmx.shield.core

object Constants {
    const val PACKAGE_NAME = "com.hmx.shield"
    const val DB_NAME = "hmx_shield_db"
    const val SECURE_PREFS = "hmx_secure_prefs"
    const val DATASTORE_NAME = "hmx_settings"

    // Keystore aliases
    const val KEY_VAULT_AES = "hmx_vault_aes"
    const val KEY_CREDENTIAL_HMAC = "hmx_credential_hmac"

    // Secure preference keys
    const val PREF_CREDENTIAL_HASH = "credential_hash"
    const val PREF_CREDENTIAL_SALT = "credential_salt"
    const val PREF_LOCK_TYPE = "lock_type"
    const val PREF_SETUP_COMPLETE = "setup_complete"
    const val PREF_INTRUDER_ENABLED = "intruder_enabled"
    const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
    const val PREF_SCREENSHOT_PROTECTION = "screenshot_protection"
    const val PREF_THEME_MODE = "theme_mode"
    const val PREF_ACCENT = "accent_color"

    // Lock activity contract extras
    const val EXTRA_LOCK_PACKAGE = "extra_lock_package"
    const val EXTRA_LOCK_APP_NAME = "extra_lock_app_name"

    // Session defaults
    const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
    const val MAX_FAILED_ATTEMPTS = 3
    const val CRASH_RETRY_LIMIT = 2
}
