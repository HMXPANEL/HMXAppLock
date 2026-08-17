package com.hmx.shield.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hmx.shield.data.local.repository.SettingsRepository
import com.hmx.shield.ui.components.GlassCard
import com.hmx.shield.ui.components.GlowButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val security: StateFlow<com.hmx.shield.data.local.db.entity.SecuritySettingsEntity?> =
        settingsRepository.observeSecurity().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setIntruder(enabled: Boolean) = run { viewModelScope.launch { settingsRepository.setIntruderEnabled(enabled) } }
    fun setBiometric(enabled: Boolean) = run { viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) } }
    fun setScreenshot(enabled: Boolean) = run { viewModelScope.launch { settingsRepository.setScreenshotProtection(enabled) } }
    fun setStealth(enabled: Boolean) = run { viewModelScope.launch { settingsRepository.setStealthMode(enabled) } }
}

@Composable
fun SettingsScreen(nav: androidx.navigation.NavHostController, vm: SettingsViewModel = hiltViewModel()) {
    val security by vm.security.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        SettingToggle("Intruder detection", "Capture a photo after failed attempts", security?.intruderDetectionEnabled ?: false) { vm.setIntruder(it) }
        SettingToggle("Biometric unlock", "Allow fingerprint to unlock", security?.biometricEnabled ?: false) { vm.setBiometric(it) }
        SettingToggle("Screenshot protection", "Block screenshots of this app", security?.screenshotProtectionEnabled ?: true) { vm.setScreenshot(it) }
        SettingToggle("Stealth mode", "Hide HMX Shield from recent apps (requires manual restart)", security?.stealthMode ?: false) { vm.setStealth(it) }

        Spacer(modifier = Modifier.height(24.dp))
        GlowButton(
            text = "Trigger test crash",
            onClick = { throw RuntimeException("Manual test crash from Settings") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}
