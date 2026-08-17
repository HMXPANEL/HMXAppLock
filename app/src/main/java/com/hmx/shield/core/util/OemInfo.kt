package com.hmx.shield.core.util

import android.os.Build

/**
 * Detects the device OEM and exposes guidance used by the Permission Center and
 * OEM recovery flows. All behavior is advisory — we never silently alter system
 * settings; we only open the relevant Settings screens.
 */
object OemInfo {
    val manufacturer: String = Build.MANUFACTURER.orEmpty().lowercase()
    val brand: String = Build.BRAND.orEmpty().lowercase()
    val model: String = Build.MODEL.orEmpty()

    val isXiaomi: Boolean get() = manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
    val isSamsung: Boolean get() = manufacturer.contains("samsung")
    val isOppo: Boolean get() = manufacturer.contains("oppo") || manufacturer.contains("oneplus")
    val isVivo: Boolean get() = manufacturer.contains("vivo")
    val isHuawei: Boolean get() = manufacturer.contains("huawei") || manufacturer.contains("honor")
    val isPixel: Boolean get() = manufacturer.contains("google")

    val knownOem: Boolean get() = isXiaomi || isSamsung || isOppo || isVivo || isHuawei || isPixel

    /**
     * Short, friendly guidance shown in the Security Center for the detected OEM.
     */
    fun batteryOptimizationGuidance(): String = when {
        isXiaomi -> "Xiaomi (MIUI/HyperOS) may stop protection in the background. Enable Auto-start, disable battery optimization, and lock HMX Shield in recent apps."
        isOppo -> "ColorOS may delay overlays and restrict background activity. Allow background activity and floating windows."
        isVivo -> "FuntouchOS may reset permissions and kill services. Enable auto-launch and disable battery restrictions."
        isHuawei -> "EMUI may restrict background execution. Allow app launch on startup and ignore battery optimization."
        isSamsung -> "Samsung may put HMX Shield to sleep. Remove it from Sleeping apps and allow unrestricted battery."
        else -> "Some devices restrict background apps. Disable battery optimization for HMX Shield for reliable protection."
    }
}
