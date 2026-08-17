package com.hmx.shield.features.authentication

import android.content.Context
import androidx.biometric.BiometricManager
import com.hmx.shield.core.model.AuthResult
import com.hmx.shield.core.model.LockType
import com.hmx.shield.core.security.CredentialManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over a single authentication method. Future methods (e.g. a future
 * GestureAuthenticator) implement this same contract so the lock screen and
 * security engine never need to know the concrete method.
 */
interface AuthenticationEngine {
    val type: LockType
    suspend fun authenticate(input: String): AuthResult
}

class PinAuthenticator @Inject constructor(
    private val credentialManager: CredentialManager
) : AuthenticationEngine {
    override val type = LockType.PIN
    override suspend fun authenticate(input: String): AuthResult =
        if (credentialManager.verify(input)) AuthResult.Success else AuthResult.Failure("Incorrect PIN")
}

class PasswordAuthenticator @Inject constructor(
    private val credentialManager: CredentialManager
) : AuthenticationEngine {
    override val type = LockType.PASSWORD
    override suspend fun authenticate(input: String): AuthResult =
        if (credentialManager.verify(input)) AuthResult.Success else AuthResult.Failure("Incorrect password")
}

class PatternAuthenticator @Inject constructor(
    private val credentialManager: CredentialManager
) : AuthenticationEngine {
    override val type = LockType.PATTERN
    override suspend fun authenticate(input: String): AuthResult =
        if (credentialManager.verify(input)) AuthResult.Success else AuthResult.Failure("Incorrect pattern")
}

/**
 * Biometric is never the sole stored secret. It is verified by the system
 * BiometricPrompt in the UI; this engine only reports availability and is used as
 * a convenience unlock that still requires the knowledge factor to have been set.
 */
class BiometricAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthenticationEngine {
    override val type = LockType.BIOMETRIC

    fun canAuthenticate(): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate(input: String): AuthResult =
        if (canAuthenticate()) AuthResult.Success else AuthResult.LockedOut
}

@Singleton
class AuthenticationManager @Inject constructor(
    private val pin: PinAuthenticator,
    private val password: PasswordAuthenticator,
    private val pattern: PatternAuthenticator
) {
    fun engineFor(type: LockType): AuthenticationEngine = when (type) {
        LockType.PIN -> pin
        LockType.PASSWORD -> password
        LockType.PATTERN -> pattern
        LockType.BIOMETRIC -> pin // biometric is a shortcut; falls back to PIN/pattern
    }
}
