package com.hmx.shield.features.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hmx.shield.core.Constants
import com.hmx.shield.core.model.AuthResult
import com.hmx.shield.core.model.LockType
import com.hmx.shield.core.security.CredentialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val credentialManager: CredentialManager,
    private val authenticationManager: AuthenticationManager
) : ViewModel() {

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val lockType: LockType get() = credentialManager.getLockType() ?: LockType.PIN

    fun setInput(value: String) {
        _input.value = value
        _error.value = null
    }

    /**
     * Validates the current input. On success [onSuccess] is invoked. On reaching
     * the failure threshold [onIntruder] is invoked with the attempt count.
     */
    fun submit(onSuccess: () -> Unit, onIntruder: (Int) -> Unit) {
        viewModelScope.launch {
            val engine = authenticationManager.engineFor(lockType)
            when (val result = engine.authenticate(_input.value)) {
                is AuthResult.Success -> {
                    _failedAttempts.value = 0
                    onSuccess()
                }
                is AuthResult.Failure -> {
                    _failedAttempts.value += 1
                    _error.value = result.reason
                    _input.value = ""
                    if (_failedAttempts.value >= Constants.MAX_FAILED_ATTEMPTS) {
                        onIntruder(_failedAttempts.value)
                    }
                }
                else -> {
                    _error.value = "Authentication unavailable"
                }
            }
        }
    }
}
