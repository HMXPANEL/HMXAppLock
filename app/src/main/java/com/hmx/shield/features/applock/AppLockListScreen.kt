package com.hmx.shield.features.applock

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hmx.shield.core.model.RelockPolicy
import com.hmx.shield.core.util.PackageUtils
import com.hmx.shield.data.local.repository.LockedAppRepository
import com.hmx.shield.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppListItem(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean,
    val relockPolicy: RelockPolicy
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LockedAppRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<AppListItem>>(emptyList())
    val items: StateFlow<List<AppListItem>> = _items.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val lockedFromRepo = repository.observeAll().first()
            val lockedMap = lockedFromRepo.associateBy { it.packageName }
            val apps = PackageUtils.listLaunchableApps(context)
            _items.value = apps.map { info ->
                val entity = lockedMap[info.packageName]
                AppListItem(
                    packageName = info.packageName,
                    appName = info.appName,
                    isLocked = entity?.isEnabled ?: false,
                    relockPolicy = entity?.relockPolicy?.let { runCatching { RelockPolicy.valueOf(it) }.getOrDefault(RelockPolicy.INSTANT) }
                        ?: RelockPolicy.INSTANT
                )
            }
        }
    }

    fun toggle(packageName: String, appName: String, locked: Boolean) {
        viewModelScope.launch {
            val existing = repository.get(packageName)
            if (existing != null) {
                repository.upsert(existing.copy(isEnabled = locked))
            } else if (locked) {
                repository.add(packageName, appName, RelockPolicy.INSTANT)
            }
            repository.loadIntoCache()
            load()
        }
    }

    fun cyclePolicy(packageName: String) {
        viewModelScope.launch {
            val existing = repository.get(packageName) ?: return@launch
            val next = when (RelockPolicy.valueOf(existing.relockPolicy)) {
                RelockPolicy.INSTANT -> RelockPolicy.SCREEN_OFF
                RelockPolicy.SCREEN_OFF -> RelockPolicy.TIMEOUT
                RelockPolicy.TIMEOUT -> RelockPolicy.INSTANT
            }
            repository.upsert(existing.copy(relockPolicy = next.name))
            repository.loadIntoCache()
            load()
        }
    }
}

@Composable
fun AppLockListScreen(nav: androidx.navigation.NavHostController, vm: AppLockViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("App Lock", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Toggle apps to protect. Tap the policy chip to change re-lock behaviour.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items) { app ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { vm.cyclePolicy(app.packageName) }) {
                                Text("Relock: ${app.relockPolicy.name}")
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = app.isLocked,
                            onCheckedChange = { vm.toggle(app.packageName, app.appName, it) }
                        )
                    }
                }
            }
        }
    }
}
