package com.hmx.shield.crash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavHostController
import com.hmx.shield.crash.CrashReportFormatter
import com.hmx.shield.crash.CrashReportRepository
import com.hmx.shield.crash.model.CrashReport
import com.hmx.shield.ui.components.GlassCard
import com.hmx.shield.ui.components.GlowButton
import com.hmx.shield.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrashReportViewModel @Inject constructor(
    private val repository: CrashReportRepository
) : ViewModel() {
    private val _pending = MutableStateFlow<CrashReport?>(null)
    val pending: StateFlow<CrashReport?> = _pending.asStateFlow()

    init { refresh() }

    fun refresh() {
        _pending.value = repository.getPending()
    }

    fun dismiss() {
        repository.dismissPending()
        _pending.value = null
    }

    fun clearAll() {
        repository.clearAll()
        _pending.value = null
    }
}

@Composable
fun CrashReportScreen(nav: NavHostController, vm: CrashReportViewModel = hiltViewModel()) {
    val report by vm.pending.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Crash Report", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "The app recovered from a problem and started safely. This report stays on your device; nothing is sent anywhere.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (report == null) {
            Text("No pending crash report. You're all set.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(listOf(report!!)) { r ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(r.timestamp))} • ${r.exceptionClass}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(r.message, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(CrashReportFormatter.format(r), style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { vm.dismiss() }) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        GlowButton(
            text = "Continue to app",
            onClick = { nav.navigate(NavRoutes.DASHBOARD) { popUpTo(NavRoutes.CRASH) { inclusive = true } } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
