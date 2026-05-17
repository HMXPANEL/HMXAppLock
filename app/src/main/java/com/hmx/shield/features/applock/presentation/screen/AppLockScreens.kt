package com.hmx.shield.features.applock.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hmx.shield.core.components.*
import com.hmx.shield.core.theme.*
import com.hmx.shield.features.applock.domain.model.LockType
import com.hmx.shield.features.applock.domain.model.RelockPolicy
import com.hmx.shield.features.applock.presentation.viewmodel.AppLockViewModel
import com.hmx.shield.features.applock.presentation.viewmodel.InstalledApp

// ══════════════════════════════════════════════════════════════════════════════
// APP LOCK LIST SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockListScreen(
    onNavigateBack: () -> Unit,
    onConfigureApp: (String) -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Lock", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        },
        containerColor = BgPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value         = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder   = { Text("Search apps…", color = TextMuted) },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentPurple,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = AccentPurple
                ),
                shape  = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Stats strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip("${state.lockedApps.size} Protected", ChipStatus.ACTIVE)
                StatusChip("${state.installedApps.size} Installed", ChipStatus.NEUTRAL)
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingDots()
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredApps, key = { it.packageName }) { app ->
                        AppRow(
                            app            = app,
                            onToggle       = { viewModel.toggleLock(app) },
                            onConfigure    = { onConfigureApp(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    onToggle: () -> Unit,
    onConfigure: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(
                1.dp,
                if (app.isLocked) AccentPurple.copy(alpha = 0.3f) else BorderColor,
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App icon placeholder
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (app.isLocked) AccentPurple.copy(0.15f) else SurfaceColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text     = app.appName.take(1).uppercase(),
                color    = if (app.isLocked) AccentPurple else TextSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(app.appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(app.packageName, color = TextMuted, fontSize = 11.sp, maxLines = 1)
        }

        if (app.isLocked) {
            IconButton(onClick = onConfigure, modifier = Modifier.size(32.dp)) {
                Text("⚙", fontSize = 16.sp, color = TextSecondary)
            }
        }

        Switch(
            checked         = app.isLocked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(
                checkedThumbColor       = Color.White,
                checkedTrackColor       = AccentPurple,
                uncheckedThumbColor     = TextMuted,
                uncheckedTrackColor     = SurfaceColor
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// APP LOCK CONFIG SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockConfigScreen(
    packageName: String,
    onNavigateBack: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val state by viewModel.configState.collectAsStateWithLifecycle()

    LaunchedEffect(packageName) { viewModel.loadAppConfig(packageName) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.app?.appName ?: "Configure Lock", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        },
        containerColor = BgPrimary
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Lock Type ─────────────────────────────────────────────────
            item {
                ShieldCard {
                    Text("LOCK TYPE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    LockType.entries.forEach { type ->
                        val selected = state.selectedLockType == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.onLockTypeSelected(type) }
                                .background(if (selected) AccentPurple.copy(0.12f) else Color.Transparent)
                                .padding(10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                type.key.replaceFirstChar { it.uppercase() },
                                color      = if (selected) AccentPurple else TextPrimary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (selected) Text("✓", color = AccentPurple)
                        }
                    }
                }
            }

            // ── Relock Policy ──────────────────────────────────────────────
            item {
                ShieldCard {
                    Text("RELOCK POLICY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("When should this app lock again?", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val policyDescriptions = mapOf(
                        RelockPolicy.INSTANT    to "Locks immediately when you leave",
                        RelockPolicy.SCREEN_OFF to "Locks when screen turns off",
                        RelockPolicy.TIMEOUT    to "Locks after a time delay"
                    )

                    RelockPolicy.entries.forEach { policy ->
                        val selected = state.selectedPolicy == policy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.onPolicySelected(policy) }
                                .background(if (selected) AccentPurple.copy(0.12f) else Color.Transparent)
                                .padding(10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    policy.key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    color      = if (selected) AccentPurple else TextPrimary,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(policyDescriptions[policy] ?: "", color = TextMuted, fontSize = 11.sp)
                            }
                            if (selected) Text("✓", color = AccentPurple)
                        }
                    }

                    // Timeout slider
                    AnimatedVisibility(visible = state.selectedPolicy == RelockPolicy.TIMEOUT) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text("Timeout: ${state.timeoutMinutes} min", color = TextSecondary, fontSize = 13.sp)
                            Slider(
                                value         = state.timeoutMinutes.toFloat(),
                                onValueChange = { viewModel.onTimeoutChanged(it.toInt()) },
                                valueRange    = 1f..60f,
                                steps         = 59,
                                colors        = SliderDefaults.colors(
                                    thumbColor       = AccentPurple,
                                    activeTrackColor = AccentPurple
                                )
                            )
                        }
                    }
                }
            }

            // ── Save ──────────────────────────────────────────────────────
            item {
                GlowButton(
                    text     = if (state.isSaving) "Saving…" else "Save Configuration",
                    enabled  = !state.isSaving,
                    onClick  = { viewModel.saveConfig(packageName, state.app?.appName ?: packageName) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
        }
    }
}
