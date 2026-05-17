package com.hmx.shield.features.permissions.presentation.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hmx.shield.core.components.GlowButton
import com.hmx.shield.core.components.StatusChip
import com.hmx.shield.core.components.ChipStatus
import com.hmx.shield.core.theme.*
import com.hmx.shield.system.monitoring.PermissionMonitor
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class PermissionSetupViewModel @Inject constructor(
    private val permissionMonitor: PermissionMonitor
) : ViewModel() {

    private val _allGranted = MutableStateFlow(false)
    val allGranted: StateFlow<Boolean> = _allGranted.asStateFlow()

    val permissionStatus = permissionMonitor.status

    fun refresh() {
        permissionMonitor.refresh()
        val s = permissionMonitor.status.value
        _allGranted.value = s.accessibilityEnabled && s.overlayGranted
    }
}

// ─── Permission Item Data ─────────────────────────────────────────────────────

private data class PermItem(
    val emoji: String,
    val title: String,
    val reason: String,
    val critical: Boolean,
    val isGranted: (com.hmx.shield.system.monitoring.PermissionStatus) -> Boolean,
    val action: (android.content.Context) -> Unit
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun PermissionSetupScreen(
    onPermissionsReady: () -> Unit,
    viewModel: PermissionSetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val status by viewModel.permissionStatus.collectAsStateWithLifecycle()

    // Refresh every time we come back to this screen (user may have changed settings)
    LaunchedEffect(Unit) { viewModel.refresh() }

    val permissions = remember {
        listOf(
            PermItem(
                emoji    = "♿",
                title    = "Accessibility Service",
                reason   = "Detects when protected apps open so the lock screen can appear instantly.",
                critical = true,
                isGranted = { it.accessibilityEnabled },
                action   = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            ),
            PermItem(
                emoji    = "🪟",
                title    = "Display Over Other Apps",
                reason   = "Required to show the lock screen on top of protected apps.",
                critical = true,
                isGranted = { it.overlayGranted },
                action   = { ctx ->
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${ctx.packageName}"))
                    )
                }
            ),
            PermItem(
                emoji    = "📊",
                title    = "Usage Access",
                reason   = "Backup method to detect foreground app on some devices.",
                critical = false,
                isGranted = { it.usageAccessGranted },
                action   = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            ),
            PermItem(
                emoji    = "🔋",
                title    = "Battery Optimization Exempt",
                reason   = "Prevents the system from killing the lock engine in the background.",
                critical = false,
                isGranted = { it.batteryOptimizationExempt },
                action   = { ctx ->
                    ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${ctx.packageName}"))
                    )
                }
            )
        )
    }

    val coreGranted = status.accessibilityEnabled && status.overlayGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        Text("🔐", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Permission Setup", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Grant permissions one at a time.\nWe only ask for what we need.",
            color     = TextSecondary,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        permissions.forEach { perm ->
            val granted = perm.isGranted(status)
            PermissionRow(
                item    = perm,
                granted = granted,
                onGrant = {
                    perm.action(context)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Refresh button
        OutlinedButton(
            onClick  = { viewModel.refresh() },
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple),
            border   = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Text("↻  Refresh Status", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = coreGranted) {
            GlowButton(
                text     = "Continue →",
                onClick  = onPermissionsReady,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
        }

        if (!coreGranted) {
            Text(
                "Accessibility + Overlay permissions are required to continue.",
                color     = TextMuted,
                fontSize  = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun PermissionRow(
    item: PermItem,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, if (granted) ColorSuccess.copy(alpha = 0.3f) else BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (granted) ColorSuccess.copy(alpha = 0.12f) else AccentPurple.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (granted) "✓" else item.emoji, fontSize = 20.sp, color = if (granted) ColorSuccess else TextPrimary)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (item.critical) StatusChip("Required", ChipStatus.CRITICAL)
            }
            Text(item.reason, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }

        if (!granted) {
            TextButton(onClick = onGrant) {
                Text("Enable", color = AccentPurple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
