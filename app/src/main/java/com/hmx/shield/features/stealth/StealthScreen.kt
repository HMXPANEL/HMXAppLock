package com.hmx.shield.features.stealth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hmx.shield.ui.components.GlassCard
import com.hmx.shield.ui.components.GlowButton
import com.hmx.shield.ui.navigation.NavRoutes

@Composable
fun StealthScreen(nav: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Stealth Mode", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Stealth Mode hides HMX Shield from the recent-apps list and can disguise " +
                        "the app icon. Enable it from Settings, then fully restart the app so the " +
                        "system applies the new task-affinity and icon state. Note: hiding the icon " +
                        "means you must dial a secret code or use a shortcut to reopen the app.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        GlowButton(text = "Open Settings", onClick = { nav.navigate(NavRoutes.SETTINGS) }, modifier = Modifier.fillMaxWidth())
    }
}
