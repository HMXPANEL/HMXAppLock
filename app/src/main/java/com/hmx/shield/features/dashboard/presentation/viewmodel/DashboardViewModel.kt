package com.hmx.shield.features.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hmx.shield.core.security.LockStateManager
import com.hmx.shield.core.security.SecurityHealth
import com.hmx.shield.features.applock.domain.model.LockedApp
import com.hmx.shield.features.applock.domain.usecase.GetLockedAppsUseCase
import com.hmx.shield.system.monitoring.PermissionMonitor
import com.hmx.shield.system.monitoring.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val lockedApps: List<LockedApp>        = emptyList(),
    val permissionStatus: PermissionStatus? = null,
    val securityScore: Int                  = 0,
    val securityHealth: SecurityHealth      = SecurityHealth.HEALTHY,
    val isLoading: Boolean                  = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getLockedAppsUseCase: GetLockedAppsUseCase,
    private val permissionMonitor: PermissionMonitor,
    private val lockStateManager: LockStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeLockedApps()
        observePermissions()
        observeSecurityHealth()
        permissionMonitor.refresh()
    }

    private fun observeLockedApps() {
        viewModelScope.launch {
            getLockedAppsUseCase()
                .collect { apps ->
                    _uiState.update { it.copy(lockedApps = apps, isLoading = false) }
                }
        }
    }

    private fun observePermissions() {
        viewModelScope.launch {
            permissionMonitor.status.collect { status ->
                val score = calculateSecurityScore(status, _uiState.value.lockedApps)
                _uiState.update { it.copy(permissionStatus = status, securityScore = score) }
            }
        }
    }

    private fun observeSecurityHealth() {
        viewModelScope.launch {
            lockStateManager.securityHealth.collect { health ->
                _uiState.update { it.copy(securityHealth = health) }
            }
        }
    }

    fun refresh() {
        permissionMonitor.refresh()
    }

    private fun calculateSecurityScore(
        status: PermissionStatus,
        apps: List<LockedApp>
    ): Int {
        var score = 0
        if (status.accessibilityEnabled)      score += 30
        if (status.overlayGranted)             score += 25
        if (status.usageAccessGranted)         score += 15
        if (status.batteryOptimizationExempt)  score += 15
        if (apps.isNotEmpty())                 score += 15
        return score.coerceIn(0, 100)
    }
}
