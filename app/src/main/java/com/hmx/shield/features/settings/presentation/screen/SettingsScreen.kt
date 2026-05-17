package com.hmx.shield.features.settings.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hmx.shield.core.components.ShieldCard
import com.hmx.shield.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    var stealthModeEnabled by remember { mutableStateOf(false) }
    var biometricEnabled   by remember { mutableStateOf(true) }
    var intruderDetection  by remember { mutableStateOf(true) }
    var showAbout          by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        },
        containerColor = BgPrimary
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Security ──────────────────────────────────────────────────
            item {
                SettingsGroup(title = "SECURITY") {
                    SettingsToggleRow(
                        emoji   = "🔐",
                        title   = "Biometric Unlock",
                        subtitle = "Use fingerprint to unlock protected apps",
                        checked  = biometricEnabled,
                        onToggle = { biometricEnabled = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        emoji    = "📸",
                        title    = "Intruder Detection",
                        subtitle = "Capture selfie after failed unlock attempts",
                        checked  = intruderDetection,
                        onToggle = { intruderDetection = it }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        emoji    = "🔑",
                        title    = "Change Master PIN",
                        subtitle = "Update your lock screen PIN",
                        onClick  = { /* navigate to PIN change */ }
                    )
                }
            }

            // ── Stealth ───────────────────────────────────────────────────
            item {
                SettingsGroup(title = "STEALTH") {
                    SettingsToggleRow(
                        emoji    = "👻",
                        title    = "Stealth Mode",
                        subtitle = "Hide app icon from launcher",
                        checked  = stealthModeEnabled,
                        onToggle = { stealthModeEnabled = it }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        emoji    = "🧮",
                        title    = "Fake Calculator",
                        subtitle = "Launch HMX Shield via calculator disguise",
                        onClick  = { /* enable fake calculator */ }
                    )
                }
            }

            // ── App Lock Behavior ─────────────────────────────────────────
            item {
                SettingsGroup(title = "APP LOCK BEHAVIOR") {
                    SettingsActionRow(
                        emoji    = "⏱",
                        title    = "Default Relock Policy",
                        subtitle = "Instant relock",
                        onClick  = { /* open policy picker */ }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        emoji    = "🔓",
                        title    = "Clear All Sessions",
                        subtitle = "Force relock all unlocked apps now",
                        onClick  = { /* clear sessions */ }
                    )
                }
            }

            // ── Data ──────────────────────────────────────────────────────
            item {
                SettingsGroup(title = "DATA & PRIVACY") {
                    SettingsActionRow(
                        emoji    = "📤",
                        title    = "Export Backup",
                        subtitle = "Encrypted local backup only — no cloud",
                        onClick  = { /* export backup */ }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        emoji    = "🗑",
                        title    = "Clear All Data",
                        subtitle = "Remove all locks, sessions and vault files",
                        onClick  = { /* confirm and clear */ },
                        destructive = true
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────
            item {
                SettingsGroup(title = "ABOUT") {
                    SettingsActionRow(
                        emoji    = "🛡",
                        title    = "HMX Shield",
                        subtitle = "Version 1.0.0 • Local-first privacy",
                        onClick  = { showAbout = !showAbout }
                    )
                    if (showAbout) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "HMX Shield is a 100% offline app lock and privacy tool.\n" +
                            "No analytics. No cloud. No tracking.\n" +
                            "All data lives only on your device.",
                            color    = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Settings Components ──────────────────────────────────────────────────────

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text     = title,
            color    = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        ShieldCard(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    emoji: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = AccentPurple,
                uncheckedTrackColor = SurfaceColor
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (destructive) ColorError else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Text("›", color = TextMuted, fontSize = 20.sp)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(vertical = 8.dp),
        color     = BorderColor,
        thickness = 0.5.dp
    )
}
