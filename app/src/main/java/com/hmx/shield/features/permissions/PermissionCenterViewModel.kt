package com.hmx.shield.features.permissions

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hmx.shield.core.util.OemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionItem(
    val key: String,
    val title: String,
    val description: String,
    val severity: String, // CRITICAL / HIGH / MEDIUM / OPTIONAL
    val granted: Boolean,
    val recoveryIntent: Intent?,
    val runtimePermission: String? = null
)

@HiltViewModel
class PermissionCenterViewModel @Inject constructor(
    private val checker: PermissionChecker
) : ViewModel() {

    private val _items = MutableStateFlow<List<PermissionItem>>(emptyList())
    val items: StateFlow<List<PermissionItem>> = _items.asStateFlow()

    private val _oemGuidance = MutableStateFlow("")
    val oemGuidance: StateFlow<String> = _oemGuidance.asStateFlow()

    init {
        _oemGuidance.value = OemInfo.batteryOptimizationGuidance()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _items.value = listOf(
                PermissionItem(
                    "accessibility",
                    "Protection Engine (Accessibility)",
                    "Detects the foreground app so the lock screen can appear.",
                    "CRITICAL",
                    checker.isAccessibilityEnabled(),
                    checker.accessibilitySettingsIntent()
                ),
                PermissionItem(
                    "usage",
                    "Usage Access (Fallback)",
                    "Backup foreground detection if Accessibility is turned off.",
                    "HIGH",
                    checker.isUsageAccessGranted(),
                    checker.usageAccessSettingsIntent()
                ),
                PermissionItem(
                    "battery",
                    "Battery Optimization",
                    "Disable to keep protection running in the background.",
                    "HIGH",
                    checker.isBatteryOptimizationIgnored(),
                    checker.batterySettingsIntent()
                ),
                PermissionItem(
                    "notifications",
                    "Notifications",
                    "Security and intruder alerts.",
                    "MEDIUM",
                    checker.areNotificationsGranted(),
                    checker.notificationSettingsIntent(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ),
                PermissionItem(
                    "biometric",
                    "Biometric",
                    "Fingerprint unlock (optional convenience).",
                    "MEDIUM",
                    checker.isBiometricAvailable(),
                    null
                ),
                PermissionItem(
                    "camera",
                    "Camera",
                    "Capture intruder selfies (optional).",
                    "OPTIONAL",
                    checker.isCameraGranted(),
                    checker.appDetailsIntent(),
                    android.Manifest.permission.CAMERA
                )
            )
        }
    }
}
