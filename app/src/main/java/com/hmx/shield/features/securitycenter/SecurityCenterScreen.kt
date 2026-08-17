package com.hmx.shield.features.securitycenter

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hmx.shield.features.permissions.PermissionCenterViewModel
import com.hmx.shield.features.permissions.PermissionItem
import com.hmx.shield.ui.components.GlassCard

@Composable
fun SecurityCenterScreen(nav: androidx.navigation.NavHostController, vm: PermissionCenterViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val guidance by vm.oemGuidance.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val runtimeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refresh()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Security Center", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Resolve any CRITICAL or HIGH items below to keep protection reliable.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))

        items.forEach { item ->
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("${item.title}  [${item.severity}]", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val status = if (item.granted) "Granted" else "Not granted"
                    Text(
                        status,
                        color = if (item.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (!item.granted && (item.runtimePermission != null || item.recoveryIntent != null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            when {
                                item.runtimePermission != null -> runtimeLauncher.launch(item.runtimePermission)
                                item.recoveryIntent != null -> context.startActivity(item.recoveryIntent)
                            }
                        }) { Text("Fix") }
                    }
                }
            }
        }

        if (guidance.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(guidance, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { vm.refresh() }) { Text("Re-check permissions") }
    }
}
