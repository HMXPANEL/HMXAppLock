package com.hmx.shield.features.securitycenter.presentation.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hmx.shield.core.components.*
import com.hmx.shield.core.security.OemCompatibilityManager
import com.hmx.shield.core.security.SecurityHealth
import com.hmx.shield.core.theme.*
import com.hmx.shield.system.monitoring.PermissionMonitor
import com.hmx.shield.system.monitoring.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    val permissionMonitor: PermissionMonitor,
    val oemManager: OemCompatibilityManager
) : ViewModel() {

    val permissionStatus = permissionMonitor.status

    fun refresh() = permissionMonitor.refresh()
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    onNavigateBack: () -> Unit,
    viewModel: SecurityCenterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val status  by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val oem     = viewModel.oemManager.currentProfile

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Center", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("Refresh", color = AccentPurple, fontSize = 13.sp)
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

            // ── Overall Health ────────────────────────────────────────────
            item {
                val health = status.overallHealth
                ShieldCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SecurityScoreRing(
                            score = when (health) {
                                SecurityHealth.HEALTHY  -> 95
                                SecurityHealth.WARNING  -> 55
                                SecurityHealth.CRITICAL -> 20
                            },
                            size = 80.dp
                        )
                        Column {
                            Text("Protection Status", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = when (health) {
                                    SecurityHealth.HEALTHY  -> "Fully Protected"
                                    SecurityHealth.WARNING  -> "Needs Attention"
                                    SecurityHealth.CRITICAL -> "At Risk!"
                                },
                                color = when (health) {
                                    SecurityHealth.HEALTHY  -> ColorSuccess
                                    SecurityHealth.WARNING  -> ColorWarning
                                    SecurityHealth.CRITICAL -> ColorError
                                },
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (health) {
                                    SecurityHealth.HEALTHY  -> "All permissions active. Lock engine running."
                                    SecurityHealth.WARNING  -> "Some optional permissions missing."
                                    SecurityHealth.CRITICAL -> "Critical permissions missing — apps may not be locked."
                                },
                                color    = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // ── Permission Details ────────────────────────────────────────
            item {
                ShieldCard(modifier = Modifier.fillMaxWidth()) {
                    Text("PERMISSIONS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionDetailRow(
                        label   = "Accessibility Service",
                        emoji   = "♿",
                        granted = status.accessibilityEnabled,
                        critical = true,
                        onFix   = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    )
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                    PermissionDetailRow(
                        label   = "Display Over Apps",
                        emoji   = "🪟",
                        granted = status.overlayGranted,
                        critical = true,
                        onFix   = {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")))
                        }
                    )
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                    PermissionDetailRow(
                        label   = "Usage Access",
                        emoji   = "📊",
                        granted = status.usageAccessGranted,
                        critical = false,
                        onFix   = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    )
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                    PermissionDetailRow(
                        label   = "Battery Exempt",
                        emoji   = "🔋",
                        granted = status.batteryOptimizationExempt,
                        critical = false,
                        onFix   = {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")))
                        }
                    )
                }
            }

            // ── OEM Setup Guide ───────────────────────────────────────────
            item {
                ShieldCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("📱", fontSize = 22.sp)
                        Column {
                            Text("Device: ${oem.displayName}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (oem.hasAggressiveBatteryKill) {
                                Text("⚠ Aggressive battery management detected", color = ColorWarning, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("RECOMMENDED SETUP STEPS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    oem.setupSteps.forEach { step ->
                        Row(
                            modifier              = Modifier.padding(vertical = 3.dp),
                            verticalAlignment     = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("•", color = AccentPurple, fontSize = 14.sp)
                            Text(step, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }

                    if (oem.settingsActions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        oem.settingsActions.forEach { action ->
                            action.intentAction?.let { intentAction ->
                                OutlinedButton(
                                    onClick  = {
                                        try { context.startActivity(Intent(intentAction)) } catch (_: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:${context.packageName}")))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple),
                                    border   = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                                ) {
                                    Text("Open: ${action.label}", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDetailRow(
    label: String,
    emoji: String,
    granted: Boolean,
    critical: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (critical && !granted) StatusChip("Critical", ChipStatus.CRITICAL)
            }
            Text(if (granted) "Active" else "Not granted", color = if (granted) ColorSuccess else ColorError, fontSize = 11.sp)
        }
        if (!granted) {
            TextButton(onClick = onFix) {
                Text("Fix", color = AccentPurple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text("✓", color = ColorSuccess, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
