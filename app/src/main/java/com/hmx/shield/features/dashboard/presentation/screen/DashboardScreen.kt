package com.hmx.shield.features.dashboard.presentation.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hmx.shield.core.components.*
import com.hmx.shield.core.security.SecurityHealth
import com.hmx.shield.core.theme.*
import com.hmx.shield.features.applock.domain.model.LockedApp
import com.hmx.shield.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.hmx.shield.system.monitoring.PermissionStatus

@Composable
fun DashboardScreen(
    onNavigateToAppLock: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding      = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        item {
            DashboardHeader()
        }

        // ── Security Score Card ─────────────────────────────────────────────
        item {
            SecurityScoreCard(
                score  = state.securityScore,
                health = state.securityHealth,
                status = state.permissionStatus,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // ── Quick Actions ───────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(title = "QUICK ACTIONS")
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        emoji    = "🔒",
                        label    = "App Lock",
                        subtitle = "${state.lockedApps.size} protected",
                        modifier = Modifier.weight(1f),
                        onClick  = onNavigateToAppLock
                    )
                    QuickActionCard(
                        emoji    = "🛡",
                        label    = "Security",
                        subtitle = state.securityHealth.displayName(),
                        modifier = Modifier.weight(1f),
                        onClick  = onNavigateToSecurity
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        emoji    = "🗄",
                        label    = "Vault",
                        subtitle = "Coming soon",
                        modifier = Modifier.weight(1f),
                        onClick  = {}
                    )
                    QuickActionCard(
                        emoji    = "⚙️",
                        label    = "Settings",
                        subtitle = "Configure",
                        modifier = Modifier.weight(1f),
                        onClick  = onNavigateToSettings
                    )
                }
            }
        }

        // ── Locked Apps ──────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title  = "PROTECTED APPS",
                    action = {
                        TextButton(onClick = onNavigateToAppLock) {
                            Text("Manage", color = AccentPurple, fontSize = 13.sp)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (state.isLoading) {
                    LoadingDots(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                } else if (state.lockedApps.isEmpty()) {
                    EmptyLockedAppsCard(onClick = onNavigateToAppLock)
                }
            }
        }

        if (!state.isLoading && state.lockedApps.isNotEmpty()) {
            items(state.lockedApps.take(5)) { app ->
                LockedAppRow(app = app, modifier = Modifier.padding(horizontal = 20.dp))
            }
            if (state.lockedApps.size > 5) {
                item {
                    TextButton(
                        onClick  = onNavigateToAppLock,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Text(
                            "+${state.lockedApps.size - 5} more apps",
                            color    = AccentPurple,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(BgSecondary, BgPrimary))
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text("HMX Shield", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Privacy Operating Layer", color = TextSecondary, fontSize = 13.sp)
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            Text("🛡", fontSize = 36.sp)
        }
    }
}

// ─── Security Score Card ──────────────────────────────────────────────────────

@Composable
private fun SecurityScoreCard(
    score: Int,
    health: SecurityHealth,
    status: PermissionStatus?,
    modifier: Modifier = Modifier
) {
    ShieldCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SecurityScoreRing(score = score, size = 88.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text("Security Score", color = TextSecondary, fontSize = 12.sp)
                Text(
                    text       = health.displayName(),
                    color      = when (health) {
                        SecurityHealth.HEALTHY  -> ColorSuccess
                        SecurityHealth.WARNING  -> ColorWarning
                        SecurityHealth.CRITICAL -> ColorError
                    },
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                status?.let { s ->
                    PermissionDot("Accessibility", s.accessibilityEnabled)
                    PermissionDot("Overlay", s.overlayGranted)
                    PermissionDot("Battery Exempt", s.batteryOptimizationExempt)
                }
            }
        }
    }
}

@Composable
private fun PermissionDot(label: String, granted: Boolean) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier              = Modifier.padding(vertical = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (granted) ColorSuccess else ColorError)
        )
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

private fun SecurityHealth.displayName() = when (this) {
    SecurityHealth.HEALTHY  -> "Protected"
    SecurityHealth.WARNING  -> "Needs Attention"
    SecurityHealth.CRITICAL -> "At Risk"
}

// ─── Quick Action Card ────────────────────────────────────────────────────────

@Composable
private fun QuickActionCard(
    emoji: String,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ShieldCard(modifier = modifier, onClick = onClick) {
        Text(emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = TextMuted, fontSize = 11.sp)
    }
}

// ─── Locked App Row ───────────────────────────────────────────────────────────

@Composable
private fun LockedAppRow(app: LockedApp, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text("🔒", fontSize = 18.sp) }

        Column(modifier = Modifier.weight(1f)) {
            Text(app.appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(app.packageName, color = TextMuted, fontSize = 11.sp, maxLines = 1)
        }

        StatusChip(
            label  = app.lockType.key.uppercase(),
            status = ChipStatus.ACTIVE
        )
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyLockedAppsCard(onClick: () -> Unit) {
    ShieldCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Text("🔓", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No apps protected yet", color = TextSecondary, fontSize = 14.sp)
            Text("Tap to add your first app →", color = AccentPurple, fontSize = 12.sp)
        }
    }
}
