package com.hmx.shield.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hmx.shield.core.security.LockedAppCache
import com.hmx.shield.features.permissions.PermissionChecker
import com.hmx.shield.ui.components.GlassCard
import com.hmx.shield.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashAction(val route: String, val title: String, val subtitle: String)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val lockedAppCache: LockedAppCache,
    private val permissionChecker: PermissionChecker
) : ViewModel() {
    private val _lockedCount = MutableStateFlow(0)
    private val _score = MutableStateFlow(0)
    val lockedCount: StateFlow<Int> = _lockedCount.asStateFlow()
    val score: StateFlow<Int> = _score.asStateFlow()

    init {
        viewModelScope.launch {
            _lockedCount.value = lockedAppCache.snapshot().size
            var s = 0
            if (permissionChecker.isAccessibilityEnabled()) s += 50
            if (permissionChecker.isUsageAccessGranted()) s += 15
            if (permissionChecker.isBatteryOptimizationIgnored()) s += 15
            if (permissionChecker.isBiometricAvailable()) s += 10
            if (permissionChecker.isCameraGranted()) s += 10
            _score.value = s
        }
    }
}

@Composable
fun DashboardScreen(nav: NavHostController, vm: DashboardViewModel = hiltViewModel()) {
    val lockedCount by vm.lockedCount.collectAsStateWithLifecycle()
    val score by vm.score.collectAsStateWithLifecycle()

    val actions = listOf(
        DashAction(NavRoutes.APP_LOCK, "App Lock", "Choose protected apps"),
        DashAction(NavRoutes.VAULT, "Private Vault", "Hide photos & files"),
        DashAction(NavRoutes.INTRUDER, "Intruder Logs", "Failed attempts"),
        DashAction(NavRoutes.SECURITY, "Security Center", "Fix permission issues"),
        DashAction(NavRoutes.THEMES, "Themes", "Personalize"),
        DashAction(NavRoutes.STEALTH, "Stealth Mode", "Hide this app"),
        DashAction(NavRoutes.SETTINGS, "Settings", "Configure")
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            GlassCard(modifier = Modifier.weight(1f).padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("$lockedCount", style = MaterialTheme.typography.headlineLarge)
                    Text("Apps locked", style = MaterialTheme.typography.bodySmall)
                }
            }
            GlassCard(modifier = Modifier.weight(1f).padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("$score%", style = MaterialTheme.typography.headlineLarge)
                    Text("Security score", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(actions) { a ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().clickable { nav.navigate(a.route) }.padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(a.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(a.subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
                    }
                }
            }
        }
    }
}
