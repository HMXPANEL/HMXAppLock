package com.hmx.shield.features.vault

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hmx.shield.data.local.repository.VaultRepository
import com.hmx.shield.ui.components.GlassCard
import com.hmx.shield.ui.components.GlowButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun displayName(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && i >= 0) c.getString(i) else "imported"
        }
    }.getOrDefault("imported")

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository
) : ViewModel() {
    val items: StateFlow<List<com.hmx.shield.data.local.db.entity.VaultFileEntity>> =
        repository.observeAll().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun import(context: Context, uri: Uri) {
        viewModelScope.launch {
            repository.import(uri, "file", displayName(context, uri))
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch { repository.delete(id) }
    }
}

@Composable
fun VaultScreen(nav: androidx.navigation.NavHostController, vm: VaultViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.import(context, it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Private Vault", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Files are encrypted at rest on your device before being stored.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        GlowButton(text = "Import file", onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items) { file ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(file.originalName, style = MaterialTheme.typography.titleMedium)
                        Text("${(file.size / 1024)} KB • ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(file.createdAt))}", style = MaterialTheme.typography.bodySmall)
                        androidx.compose.material3.TextButton(onClick = { vm.delete(file.id) }) { Text("Delete") }
                    }
                }
            }
        }
        if (items.isEmpty()) {
            Text("No files in the vault yet.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
