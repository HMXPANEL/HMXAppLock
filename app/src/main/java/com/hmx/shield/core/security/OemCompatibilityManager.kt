package com.hmx.shield.core.security

import android.os.Build
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

// ─── OEM Types ────────────────────────────────────────────────────────────────

enum class OemType {
    XIAOMI, SAMSUNG, OPPO, VIVO, HUAWEI, ONEPLUS, PIXEL, GENERIC
}

// ─── OEM Profile ──────────────────────────────────────────────────────────────

data class OemProfile(
    val oemType: OemType,
    val displayName: String,
    /** OEM aggressively kills background services — needs strong battery exemption guidance */
    val hasAggressiveBatteryKill: Boolean,
    /** OEM causes overlay delays or blocks — needs preloaded overlay strategy */
    val hasOverlayIssues: Boolean,
    /** OEM has a proprietary Auto-Start setting that must be enabled */
    val hasAutoStartSetting: Boolean,
    /** Human-readable setup steps shown in the OEM Setup Wizard */
    val setupSteps: List<String>,
    /** Actionable settings deep-links for the Setup Wizard */
    val settingsActions: List<OemSettingsAction>
)

data class OemSettingsAction(
    val label: String,
    val intentAction: String?,
    val packageName: String? = null
)

// ─── OemCompatibilityManager ──────────────────────────────────────────────────

@Singleton
class OemCompatibilityManager @Inject constructor() {

    val currentOem: OemType = detectOem()
    val currentProfile: OemProfile = buildProfile(currentOem)

    // ─── Detection ────────────────────────────────────────────────────────────

    private fun detectOem(): OemType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return when {
            manufacturer.contains("xiaomi")
                    || brand.contains("xiaomi")
                    || brand.contains("redmi")
                    || brand.contains("poco") -> OemType.XIAOMI

            manufacturer.contains("samsung") -> OemType.SAMSUNG

            manufacturer.contains("oppo")
                    || brand.contains("oppo")
                    || brand.contains("realme") -> OemType.OPPO

            manufacturer.contains("vivo")
                    || brand.contains("vivo") -> OemType.VIVO

            manufacturer.contains("huawei")
                    || brand.contains("huawei")
                    || brand.contains("honor") -> OemType.HUAWEI

            manufacturer.contains("oneplus")
                    || brand.contains("oneplus") -> OemType.ONEPLUS

            manufacturer.contains("google") -> OemType.PIXEL

            else -> OemType.GENERIC
        }
    }

    // ─── Profile Builder ──────────────────────────────────────────────────────

    private fun buildProfile(oem: OemType): OemProfile = when (oem) {

        OemType.XIAOMI -> OemProfile(
            oemType = oem,
            displayName = "Xiaomi / HyperOS / MIUI",
            hasAggressiveBatteryKill = true,
            hasOverlayIssues = true,
            hasAutoStartSetting = true,
            setupSteps = listOf(
                "1. Enable Auto Start for HMX Shield",
                "2. Set Battery to 'No Restrictions'",
                "3. Lock HMX Shield in Recent Apps",
                "4. Allow 'Display over other apps'"
            ),
            settingsActions = listOf(
                OemSettingsAction(
                    "Auto Start Settings",
                    "miui.intent.action.OP_AUTO_START"
                ),
                OemSettingsAction(
                    "Battery Settings",
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                )
            )
        )

        OemType.SAMSUNG -> OemProfile(
            oemType = oem,
            displayName = "Samsung / One UI",
            hasAggressiveBatteryKill = false,
            hasOverlayIssues = false,
            hasAutoStartSetting = false,
            setupSteps = listOf(
                "1. Remove HMX Shield from Sleeping Apps",
                "2. Set Battery usage to Unrestricted",
                "3. Allow Background Activity"
            ),
            settingsActions = listOf(
                OemSettingsAction(
                    "Battery Settings",
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                )
            )
        )

        OemType.OPPO -> OemProfile(
            oemType = oem,
            displayName = "Oppo / ColorOS",
            hasAggressiveBatteryKill = true,
            hasOverlayIssues = true,
            hasAutoStartSetting = true,
            setupSteps = listOf(
                "1. Enable Auto Launch",
                "2. Allow Background Activity",
                "3. Disable Smart Standby",
                "4. Enable Floating Windows"
            ),
            settingsActions = listOf(
                OemSettingsAction(
                    "Battery Settings",
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                )
            )
        )

        OemType.VIVO -> OemProfile(
            oemType = oem,
            displayName = "Vivo / FuntouchOS",
            hasAggressiveBatteryKill = true,
            hasOverlayIssues = false,
            hasAutoStartSetting = true,
            setupSteps = listOf(
                "1. Enable Auto Launch",
                "2. Disable Battery Restrictions",
                "3. Allow Lock Screen Notifications"
            ),
            settingsActions = listOf(
                OemSettingsAction(
                    "Battery Settings",
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                )
            )
        )

        OemType.HUAWEI -> OemProfile(
            oemType = oem,
            displayName = "Huawei / HarmonyOS / EMUI",
            hasAggressiveBatteryKill = true,
            hasOverlayIssues = true,
            hasAutoStartSetting = true,
            setupSteps = listOf(
                "1. Enable Auto Launch",
                "2. Allow Background Activity",
                "3. Add to Protected Apps",
                "4. Disable Power Plan Restrictions"
            ),
            settingsActions = listOf(
                OemSettingsAction(
                    "Battery Settings",
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                )
            )
        )

        OemType.ONEPLUS -> OemProfile(
            oemType = oem,
            displayName = "OnePlus / OxygenOS",
            hasAggressiveBatteryKill = false,
            hasOverlayIssues = false,
            hasAutoStartSetting = false,
            setupSteps = listOf(
                "1. Set Battery to Unrestricted",
                "2. Allow Background Activity"
            ),
            settingsActions = listOf(
                OemSettingsAction(
                    "Battery Settings",
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                )
            )
        )

        OemType.PIXEL, OemType.GENERIC -> OemProfile(
            oemType = oem,
            displayName = if (oem == OemType.PIXEL) "Pixel / Stock Android" else "Android",
            hasAggressiveBatteryKill = false,
            hasOverlayIssues = false,
            hasAutoStartSetting = false,
            setupSteps = listOf("1. Disable Battery Optimization for HMX Shield"),
            settingsActions = listOf(
                OemSettingsAction(
                    "Battery Settings",
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                )
            )
        )
    }

    /** Returns true for OEMs that need aggressive service heartbeat recovery */
    fun needsAggressiveRecovery(): Boolean = currentProfile.hasAggressiveBatteryKill

    /** Returns true if this OEM commonly causes overlay delays */
    fun hasOverlayIssues(): Boolean = currentProfile.hasOverlayIssues
}
