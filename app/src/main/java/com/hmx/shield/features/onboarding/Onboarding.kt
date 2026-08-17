package com.hmx.shield.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hmx.shield.core.Constants
import com.hmx.shield.core.security.SecurePreferences
import com.hmx.shield.features.permissions.PermissionCenterViewModel
import com.hmx.shield.ui.components.GlowButton
import com.hmx.shield.ui.components.GlassCard
import com.hmx.shield.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val securePreferences: SecurePreferences
) : ViewModel() {
    private val _setupComplete = MutableStateFlow(securePreferences.getBoolean(Constants.PREF_SETUP_COMPLETE))
    val setupComplete: StateFlow<Boolean> = _setupComplete

    fun setSetupComplete() {
        viewModelScope.launch {
            securePreferences.putBoolean(Constants.PREF_SETUP_COMPLETE, true)
            _setupComplete.value = true
        }
    }
}

@Composable
fun SplashScreen(nav: NavHostController, vm: OnboardingViewModel = hiltViewModel()) {
    val setupComplete by vm.setupComplete.collectAsStateWithLifecycle()
    LaunchedEffect(setupComplete) {
        when {
            setupComplete -> nav.navigate(NavRoutes.DASHBOARD) { popUpTo(NavRoutes.SPLASH) { inclusive = true } }
            else -> nav.navigate(NavRoutes.WELCOME) { popUpTo(NavRoutes.SPLASH) { inclusive = true } }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("HMX Shield", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Securing your apps", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun WelcomeScreen(nav: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to HMX Shield", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Lock any app with a PIN, password, pattern or fingerprint. " +
                "Built privacy-first: no account, no internet, data stays on your device.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        GlowButton(text = "Get Started", onClick = { nav.navigate(NavRoutes.ONBOARDING) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun OnboardingScreen(nav: NavHostController) {
    val pages = listOf(
        "Choose how apps are locked — PIN, password, pattern or biometrics.",
        "Pick which apps to protect. Everything is encrypted on-device.",
        "If someone enters the wrong code, an intruder photo is captured (optional)."
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        pages.forEach {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        GlowButton(text = "Continue", onClick = { nav.navigate(NavRoutes.PERMISSIONS) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun PermissionSetupScreen(nav: NavHostController, vm: PermissionCenterViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val guidance by vm.oemGuidance.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Enable Protection", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "HMX Shield uses an Accessibility Service to detect the foreground app. " +
                "Grant it and the other items below to keep protection reliable.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        items.forEach { item ->
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${item.title}  [${item.severity}]", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (item.granted) "Granted" else "Not granted",
                        color = if (item.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        if (guidance.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(guidance, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Start)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        GlowButton(text = "Next", onClick = { nav.navigate(NavRoutes.CREATE_LOCK) }, modifier = Modifier.fillMaxWidth())
    }
}
