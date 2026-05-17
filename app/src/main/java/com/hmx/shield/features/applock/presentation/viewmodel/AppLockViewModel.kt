package com.hmx.shield.features.applock.presentation.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hmx.shield.features.applock.domain.model.LockedApp
import com.hmx.shield.features.applock.domain.model.LockType
import com.hmx.shield.features.applock.domain.model.RelockPolicy
import com.hmx.shield.features.applock.domain.usecase.GetLockedAppsUseCase
import com.hmx.shield.features.applock.domain.usecase.LockAppUseCase
import com.hmx.shield.features.applock.domain.usecase.UnlockAppUseCase
import com.hmx.shield.features.applock.domain.repository.AppLockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ─── Installed App Model ──────────────────────────────────────────────────────

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean
)

// ─── UI States ────────────────────────────────────────────────────────────────

data class AppLockListState(
    val installedApps: List<InstalledApp> = emptyList(),
    val lockedApps: List<LockedApp>       = emptyList(),
    val searchQuery: String               = "",
    val isLoading: Boolean                = true
) {
    val filteredApps: List<InstalledApp>
        get() = if (searchQuery.isBlank()) installedApps
                else installedApps.filter {
                    it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
                }
}

data class AppLockConfigState(
    val app: LockedApp?            = null,
    val selectedLockType: LockType     = LockType.PIN,
    val selectedPolicy: RelockPolicy   = RelockPolicy.INSTANT,
    val timeoutMinutes: Int            = 5,
    val isSaving: Boolean              = false,
    val saveSuccess: Boolean           = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AppLockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getLockedAppsUseCase: GetLockedAppsUseCase,
    private val lockAppUseCase: LockAppUseCase,
    private val repository: AppLockRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(AppLockListState())
    val listState: StateFlow<AppLockListState> = _listState.asStateFlow()

    private val _configState = MutableStateFlow(AppLockConfigState())
    val configState: StateFlow<AppLockConfigState> = _configState.asStateFlow()

    private val _lockedPackages = MutableStateFlow<Set<String>>(emptySet())

    init {
        loadInstalledApps()
        observeLockedApps()
    }

    // ─── List Screen ─────────────────────────────────────────────────────────

    private fun observeLockedApps() {
        viewModelScope.launch {
            getLockedAppsUseCase().collect { locked ->
                _lockedPackages.value = locked.map { it.packageName }.toSet()
                _listState.update { state ->
                    state.copy(
                        lockedApps    = locked,
                        installedApps = state.installedApps.map { installed ->
                            installed.copy(isLocked = installed.packageName in _lockedPackages.value)
                        }
                    )
                }
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm    = context.packageManager
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                } else null

                val allApps = if (flags != null) pm.getInstalledApplications(flags)
                              else @Suppress("DEPRECATION") pm.getInstalledApplications(PackageManager.GET_META_DATA)

                allApps
                    .filter { isUserApp(it) }
                    .filter { it.packageName != context.packageName }
                    .map { info ->
                        InstalledApp(
                            packageName = info.packageName,
                            appName     = pm.getApplicationLabel(info).toString(),
                            isLocked    = info.packageName in _lockedPackages.value
                        )
                    }
                    .sortedBy { it.appName }
            }
            _listState.update { it.copy(installedApps = apps, isLoading = false) }
        }
    }

    private fun isUserApp(info: ApplicationInfo): Boolean {
        return (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
               (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    fun onSearchQueryChanged(query: String) {
        _listState.update { it.copy(searchQuery = query) }
    }

    fun toggleLock(app: InstalledApp) {
        viewModelScope.launch {
            if (app.isLocked) {
                repository.unlockApp(app.packageName)
            } else {
                lockAppUseCase(
                    LockedApp(
                        packageName   = app.packageName,
                        appName       = app.appName,
                        lockType      = LockType.PIN,
                        relockPolicy  = RelockPolicy.INSTANT,
                        isEnabled     = true
                    )
                )
            }
        }
    }

    // ─── Config Screen ────────────────────────────────────────────────────────

    fun loadAppConfig(packageName: String) {
        viewModelScope.launch {
            val app = repository.getLockedApp(packageName)
            _configState.update {
                it.copy(
                    app               = app,
                    selectedLockType  = app?.lockType ?: LockType.PIN,
                    selectedPolicy    = app?.relockPolicy ?: RelockPolicy.INSTANT,
                    timeoutMinutes    = ((app?.relockTimeoutMs ?: 300_000L) / 60_000L).toInt()
                )
            }
        }
    }

    fun onLockTypeSelected(lockType: LockType) {
        _configState.update { it.copy(selectedLockType = lockType) }
    }

    fun onPolicySelected(policy: RelockPolicy) {
        _configState.update { it.copy(selectedPolicy = policy) }
    }

    fun onTimeoutChanged(minutes: Int) {
        _configState.update { it.copy(timeoutMinutes = minutes) }
    }

    fun saveConfig(packageName: String, appName: String) {
        viewModelScope.launch {
            _configState.update { it.copy(isSaving = true) }
            val cfg = _configState.value
            lockAppUseCase(
                LockedApp(
                    packageName      = packageName,
                    appName          = appName,
                    lockType         = cfg.selectedLockType,
                    relockPolicy     = cfg.selectedPolicy,
                    relockTimeoutMs  = cfg.timeoutMinutes * 60_000L,
                    isEnabled        = true
                )
            )
            _configState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }
}
