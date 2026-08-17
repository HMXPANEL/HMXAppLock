package com.hmx.shield.features.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hmx.shield.core.Constants
import com.hmx.shield.core.model.LockType
import com.hmx.shield.core.security.CredentialManager
import com.hmx.shield.core.security.SecurePreferences
import com.hmx.shield.features.onboarding.OnboardingViewModel
import com.hmx.shield.ui.components.GlowButton
import com.hmx.shield.ui.components.GlassCard
import com.hmx.shield.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateLockViewModel @Inject constructor(
    private val credentialManager: CredentialManager,
    private val securePreferences: SecurePreferences
) : ViewModel() {
    fun save(type: LockType, secret: String, onDone: () -> Unit) {
        viewModelScope.launch {
            credentialManager.setCredential(secret, type)
            onDone()
        }
    }
}

@Composable
fun CreateLockScreen(nav: NavHostController) {
    val vm: CreateLockViewModel = hiltViewModel()
    val setupVm: OnboardingViewModel = hiltViewModel()

    var type by remember { mutableStateOf(LockType.PIN) }
    var secret by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val types = listOf(LockType.PIN, LockType.PASSWORD, LockType.PATTERN)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create your lock", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        types.forEach { t ->
            Row(
                modifier = Modifier.fillMaxWidth().selectable(selected = type == t, onClick = { type = t }).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = type == t, onClick = { type = t })
                Spacer(modifier = Modifier.width(8.dp))
                Text(t.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = secret,
            onValueChange = { secret = it },
            label = { Text("Enter ${type.name.lowercase()}") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = if (type == LockType.PIN) KeyboardType.NumberPassword else KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirm") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = if (type == LockType.PIN) KeyboardType.NumberPassword else KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(24.dp))
        GlowButton(text = "Finish", onClick = {
            when {
                secret.length < (if (type == LockType.PASSWORD) 6 else 4) -> error = "Too short"
                secret != confirm -> error = "Does not match"
                else -> vm.save(type, secret) { setupVm.setSetupComplete(); nav.navigate(NavRoutes.DASHBOARD) { popUpTo(NavRoutes.CREATE_LOCK) { inclusive = true } } }
            }
        }, modifier = Modifier.fillMaxWidth())
    }
}
