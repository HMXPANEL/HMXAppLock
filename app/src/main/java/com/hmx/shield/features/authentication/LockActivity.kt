package com.hmx.shield.features.authentication

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hmx.shield.core.Constants.EXTRA_LOCK_APP_NAME
import com.hmx.shield.core.Constants.EXTRA_LOCK_PACKAGE
import com.hmx.shield.core.model.RelockPolicy
import com.hmx.shield.core.security.LockedAppCache
import com.hmx.shield.core.security.SessionManager
import com.hmx.shield.core.ThemeController
import com.hmx.shield.crash.CrashContextHolder
import com.hmx.shield.data.local.repository.SettingsRepository
import com.hmx.shield.features.intruder.IntruderCaptureManager
import com.hmx.shield.ui.theme.HmxTheme
import com.hmx.shield.ui.theme.LocalThemeController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LockActivity : ComponentActivity() {

    private val viewModel: LockViewModel by viewModels()

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var lockedAppCache: LockedAppCache
    @Inject lateinit var intruderCaptureManager: IntruderCaptureManager
    @Inject lateinit var biometric: BiometricAuthenticator
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var crashContextHolder: CrashContextHolder
    @Inject lateinit var themeController: ThemeController

    private lateinit var pkg: String
    private lateinit var appName: String
    private var policy: RelockPolicy = RelockPolicy.TIMEOUT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        pkg = intent.getStringExtra(EXTRA_LOCK_PACKAGE) ?: return finish()
        appName = intent.getStringExtra(EXTRA_LOCK_APP_NAME) ?: pkg
        policy = lockedAppCache.getInfo(pkg)?.relockPolicy ?: RelockPolicy.TIMEOUT
        crashContextHolder.setScreen("LockScreen:$appName")

        val root = ComposeView(this)
        setContentView(root)
        root.setContent {
            CompositionLocalProvider(LocalThemeController provides themeController) {
                HmxTheme {
                    LockScreen(
                        appName = appName,
                        viewModel = viewModel,
                        biometricAvailable = isBiometricUsable(),
                        onBiometric = { showBiometric() },
                        onUnlocked = { unlockAndFinish() },
                        onIntruder = { attempts -> handleIntruder(attempts) }
                    )
                }
            }
        }
    }

    private fun isBiometricUsable(): Boolean {
        val enabled = runCatching {
            settingsRepository.observeSecurity().firstOrNull()?.biometricEnabled ?: false
        }.getOrDefault(false)
        return enabled && biometric.canAuthenticate()
    }

    private fun unlockAndFinish() {
        sessionManager.unlock(pkg, policy)
        crashContextHolder.setScreen(null)
        finish()
    }

    private fun handleIntruder(attempts: Int) {
        lifecycleScope.launch {
            val enabled = settingsRepository.observeSecurity().firstOrNull()?.intruderDetectionEnabled ?: false
            if (enabled) intruderCaptureManager.capture(pkg, attempts)
        }
    }

    private fun showBiometric() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlockAndFinish()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock $appName")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        runCatching { prompt.authenticate(info) }
    }

    override fun onDestroy() {
        crashContextHolder.setScreen(null)
        super.onDestroy()
    }
}
