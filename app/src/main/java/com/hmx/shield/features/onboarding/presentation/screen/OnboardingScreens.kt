package com.hmx.shield.features.onboarding.presentation.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.hmx.shield.core.components.GlowButton
import com.hmx.shield.core.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// SPLASH SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SplashScreen(
    onSetupComplete: () -> Unit,
    onSetupIncomplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 0.9f,
        targetValue   = 1.1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_scale"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        // TODO: check DataStore for setup completion flag
        // For now, always go to welcome on first launch
        onSetupIncomplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgPrimary, BgSecondary))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(AccentPurple.copy(alpha = 0.4f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡", fontSize = 52.sp)
            }
            Text(
                text       = "HMX Shield",
                color      = TextPrimary,
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text  = "Privacy-First App Lock",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// WELCOME SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val features = listOf(
        Triple("🔒", "Lock Any App", "Protect your apps with PIN, pattern or fingerprint"),
        Triple("🔐", "Encrypted Vault", "Store private files with AES encryption"),
        Triple("📱", "Works Offline", "Zero cloud. Everything stays on your device")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgPrimary, BgSecondary)))
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text("🛡", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text       = "HMX Shield",
            color      = TextPrimary,
            fontSize   = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = "Your phone. Your privacy.\nNo cloud. No tracking.",
            color     = TextSecondary,
            fontSize  = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        features.forEach { (icon, title, desc) ->
            Row(
                modifier             = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 22.sp)
                }
                Column {
                    Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(desc, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        GlowButton(
            text      = "Get Started",
            onClick   = onGetStarted,
            modifier  = Modifier.fillMaxWidth().height(52.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ONBOARDING SCREEN  (4-page carousel)
// ══════════════════════════════════════════════════════════════════════════════

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage("🔒", "Lock Any App",
        "Protect WhatsApp, Instagram, banking apps and more with PIN or fingerprint."),
    OnboardingPage("🗄", "Private Vault",
        "Hide photos, videos and documents in an AES-encrypted vault invisible to gallery apps."),
    OnboardingPage("📸", "Intruder Detection",
        "Automatically capture a selfie when someone enters the wrong PIN too many times."),
    OnboardingPage("🌐", "100% Offline",
        "All data stays on your device. No cloud sync, no analytics, no tracking. Ever.")
)

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope      = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val item = onboardingPages[page]
            Column(
                modifier              = Modifier.fillMaxSize().padding(40.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.Center
            ) {
                Text(item.emoji, fontSize = 72.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text(item.title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(item.description, color = TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 23.sp)
            }
        }

        // Pager dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(onboardingPages.size) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == pagerState.currentPage) 20.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (i == pagerState.currentPage) AccentPurple else BorderColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        GlowButton(
            text     = if (pagerState.currentPage == onboardingPages.lastIndex) "Set Up Protection" else "Next",
            onClick  = {
                if (pagerState.currentPage == onboardingPages.lastIndex) {
                    onContinue()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.padding(horizontal = 40.dp).fillMaxWidth().height(52.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LOCK CREATION SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun LockCreationScreen(onLockCreated: () -> Unit) {
    var pin        by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step       by remember { mutableIntStateOf(0) } // 0=create, 1=confirm
    var error      by remember { mutableStateOf("") }
    val maxLen = 6

    Column(
        modifier              = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Text("🔑", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text       = if (step == 0) "Create Master PIN" else "Confirm Your PIN",
            color      = TextPrimary,
            fontSize   = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text    = if (step == 0) "This PIN protects all your locked apps" else "Enter the same PIN again",
            color   = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = ColorError, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))

        // PIN dots
        val current = if (step == 0) pin else confirmPin
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(maxLen) { i ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (i < current.length) AccentPurple else BorderColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Simple numpad
        val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    when {
                        key.isEmpty() -> Spacer(Modifier.size(72.dp))
                        else -> Button(
                            onClick = {
                                when (key) {
                                    "⌫" -> if (step == 0) { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                           else { if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1) }
                                    else -> {
                                        error = ""
                                        if (step == 0 && pin.length < maxLen) {
                                            pin += key
                                            if (pin.length == maxLen) step = 1
                                        } else if (step == 1 && confirmPin.length < maxLen) {
                                            confirmPin += key
                                            if (confirmPin.length == maxLen) {
                                                if (confirmPin == pin) {
                                                    // TODO: save hashed PIN to DataStore
                                                    onLockCreated()
                                                } else {
                                                    error = "PINs don't match. Try again."
                                                    pin = ""; confirmPin = ""; step = 0
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(72.dp),
                            shape    = CircleShape,
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (key == "⌫") Color.Transparent else NumKeyBg,
                                contentColor   = TextPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                        ) {
                            Text(key, fontSize = if (key == "⌫") 20.sp else 22.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
