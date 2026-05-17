package com.hmx.shield.system.overlay.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hmx.shield.features.applock.domain.model.LockType
import kotlinx.coroutines.delay

// ─── Constants ────────────────────────────────────────────────────────────────

private val BG_TOP    = Color(0xFF060611)
private val BG_BOTTOM = Color(0xFF0D1B2A)
private val ACCENT    = Color(0xFF7B61FF)
private val DOT_FILL  = ACCENT
private val DOT_EMPTY = Color(0xFF2A2A3E)
private val KEY_BG    = Color(0xFF1A1A2E)
private val KEY_TEXT  = Color(0xFFEEEEFF)
private val HINT_TEXT = Color(0x80EEEEFF)
private const val MAX_PIN = 6
private const val MAX_WRONG_ATTEMPTS = 5

// ─── Root Screen ─────────────────────────────────────────────────────────────

/**
 * Full-screen lock overlay UI.
 *
 * Handles two auth modes:
 *   - PIN (also used as Pattern fallback in Phase 2)
 *   - FINGERPRINT (shows biometric prompt + "use PIN" fallback)
 *
 * Phase 3 additions: real biometric prompt wiring, pattern grid, shake animation
 * on wrong PIN, intruder selfie trigger after [MAX_WRONG_ATTEMPTS] failures.
 */
@Composable
fun LockOverlayScreen(
    packageName: String,
    appName: String,
    lockType: LockType,
    onAuthSuccess: () -> Unit,
    onAuthFailed: () -> Unit
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(BG_TOP, BG_BOTTOM)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Shield icon pulse
            ShieldPulse()

            Spacer(modifier = Modifier.height(20.dp))

            // App name
            Text(
                text = appName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Protected by HMX Shield",
                color = HINT_TEXT,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Auth section
            when (lockType) {
                LockType.FINGERPRINT -> BiometricSection(
                    onFingerprintTap = onAuthSuccess,   // Phase 3: wire BiometricPrompt
                    onUsePinInstead  = { /* Switch to PIN inline */ }
                )
                else -> PinSection(
                    onSuccess = onAuthSuccess,
                    onFailed  = onAuthFailed
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Shield Pulse Animation ───────────────────────────────────────────────────

@Composable
private fun ShieldPulse() {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(ACCENT.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        // Phase 3: replace with actual shield vector drawable
        Text("🛡", fontSize = 32.sp)
    }
}

// ─── PIN Section ──────────────────────────────────────────────────────────────

@Composable
private fun PinSection(
    onSuccess: () -> Unit,
    onFailed: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var wrongAttempts by remember { mutableStateOf(0) }
    var shaking by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }        // true while cooldown active
    var cooldownSecs by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current

    // Cooldown timer after too many wrong attempts
    LaunchedEffect(locked) {
        if (locked) {
            cooldownSecs = 30
            while (cooldownSecs > 0) {
                delay(1000)
                cooldownSecs--
            }
            locked = false
            wrongAttempts = 0
            pinInput = ""
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Status message
        if (locked) {
            Text(
                text = "Too many attempts. Wait ${cooldownSecs}s",
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        } else if (wrongAttempts > 0) {
            Text(
                text = "Incorrect PIN (${wrongAttempts}/${MAX_WRONG_ATTEMPTS})",
                color = Color(0xFFFFB347),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Enter PIN",
                color = HINT_TEXT,
                fontSize = 14.sp
            )
        }

        // PIN dot indicators
        PinDots(filledCount = pinInput.length, total = MAX_PIN, shaking = shaking)

        Spacer(modifier = Modifier.height(8.dp))

        // Numpad — disabled during cooldown
        NumPad(
            enabled = !locked,
            onDigit = { digit ->
                if (pinInput.length < MAX_PIN) {
                    pinInput += digit
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // Auto-submit on full PIN
                    if (pinInput.length == MAX_PIN) {
                        // ─────────────────────────────────────────────────
                        // Phase 3: Replace this with real PIN verification
                        //   val correct = pinVerifier.verify(pinInput)
                        // For Phase 2 dev/testing: treat any 6-digit input
                        // as correct so you can see the full flow end-to-end.
                        // ─────────────────────────────────────────────────
                        val correct = true  // TODO: wire to PinVerifier in Phase 3

                        if (correct) {
                            onSuccess()
                            pinInput = ""
                        } else {
                            wrongAttempts++
                            shaking = true
                            onFailed()
                            pinInput = ""
                            if (wrongAttempts >= MAX_WRONG_ATTEMPTS) {
                                locked = true
                                // Phase 3: trigger intruder selfie capture here
                            }
                        }
                    }
                }
            },
            onDelete = {
                if (pinInput.isNotEmpty()) {
                    pinInput = pinInput.dropLast(1)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        )
    }

    // Reset shake flag after animation
    LaunchedEffect(shaking) {
        if (shaking) {
            delay(400)
            shaking = false
        }
    }
}

// ─── PIN Dot Row ──────────────────────────────────────────────────────────────

@Composable
private fun PinDots(filledCount: Int, total: Int, shaking: Boolean) {
    val offsetX by animateFloatAsState(
        targetValue = if (shaking) 10f else 0f,
        animationSpec = if (shaking) spring(stiffness = Spring.StiffnessHigh) else snap(),
        label = "shake_x"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.offset(x = offsetX.dp)
    ) {
        repeat(total) { index ->
            val filled = index < filledCount
            Box(
                modifier = Modifier
                    .size(if (filled) 14.dp else 12.dp)
                    .clip(CircleShape)
                    .background(if (filled) DOT_FILL else DOT_EMPTY)
            )
        }
    }
}

// ─── Numpad ───────────────────────────────────────────────────────────────────

@Composable
private fun NumPad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("",  "0", "⌫")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when {
                        key.isEmpty() -> Spacer(modifier = Modifier.size(76.dp))
                        key == "⌫"   -> NumKey(label = key, enabled = enabled, isDelete = true,  onClick = onDelete)
                        else         -> NumKey(label = key, enabled = enabled, isDelete = false, onClick = { onDigit(key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NumKey(
    label: String,
    enabled: Boolean,
    isDelete: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(76.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor  = if (isDelete) Color.Transparent else KEY_BG,
            contentColor    = KEY_TEXT,
            disabledContainerColor = KEY_BG.copy(alpha = 0.3f),
            disabledContentColor   = KEY_TEXT.copy(alpha = 0.3f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text       = label,
            fontSize   = if (isDelete) 20.sp else 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Biometric Section ────────────────────────────────────────────────────────

@Composable
private fun BiometricSection(
    onFingerprintTap: () -> Unit,
    onUsePinInstead: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text  = "Touch fingerprint sensor",
            color = HINT_TEXT,
            fontSize = 15.sp
        )

        // Fingerprint button — Phase 3: replace with BiometricPrompt trigger
        Button(
            onClick = onFingerprintTap,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ACCENT.copy(alpha = 0.2f),
                contentColor   = ACCENT
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
        ) {
            Text("◉", fontSize = 34.sp, color = ACCENT)
        }

        TextButton(onClick = onUsePinInstead) {
            Text(
                text     = "Use PIN instead",
                color    = HINT_TEXT,
                fontSize = 13.sp
            )
        }
    }
}
