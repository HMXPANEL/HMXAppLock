package com.hmx.shield.features.intruder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.hmx.shield.data.local.db.entity.IntruderLogEntity
import com.hmx.shield.data.local.repository.IntruderRepository
import com.hmx.shield.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntruderViewModel @Inject constructor(
    private val repository: IntruderRepository
) : ViewModel() {
    private val _logs = MutableStateFlow<List<IntruderLogEntity>>(emptyList())
    val logs: StateFlow<List<IntruderLogEntity>> = _logs.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch { _logs.value = repository.getAll() }
    }

    fun delete(id: Int) {
        viewModelScope.launch { repository.delete(id); load() }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clear(); load() }
    }
}

@Composable
fun IntruderLogsScreen(nav: androidx.navigation.NavHostController, vm: IntruderViewModel = hiltViewModel()) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Intruder Logs", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (logs.isNotEmpty()) {
            TextButton(onClick = { vm.clearAll() }) { Text("Clear all") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logs) { log ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(log.packageName, style = MaterialTheme.typography.titleMedium)
                        Text("Attempts: ${log.failedAttempts}  •  ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(log.timestamp))}", style = MaterialTheme.typography.bodySmall)
                        if (log.imagePath.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = java.io.File(log.imagePath),
                                contentDescription = "Intruder photo",
                                modifier = Modifier.size(96.dp)
                            )
                        }
                        TextButton(onClick = { vm.delete(log.id) }) { Text("Delete") }
                    }
                }
            }
        }
        if (logs.isEmpty()) {
            Text("No intruder attempts recorded yet.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
