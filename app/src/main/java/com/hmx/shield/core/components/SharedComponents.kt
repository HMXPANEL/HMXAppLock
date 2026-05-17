package com.hmx.shield.core.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hmx.shield.core.theme.*

// ─── ShieldCard ───────────────────────────────────────────────────────────────

/**
 * Glass-morphism style card used across all screens.
 * Subtle border + dark background + rounded corners.
 */
@Composable
fun ShieldCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .clip(RoundedCornerShape(20.dp))
        .background(CardBg)
        .border(
            width = 1.dp,
            color = BorderColor,
            shape = RoundedCornerShape(20.dp)
        )
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )
        .padding(16.dp)

    Column(modifier = baseModifier, content = content)
}

// ─── GlowButton ───────────────────────────────────────────────────────────────

/**
 * Primary CTA button with gradient background.
 * Used for main actions: "Get Started", "Enable", "Save"
 */
@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: List<Color> = GradientPurpleBlue
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue  = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label        = "glow_button_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) Brush.horizontalGradient(gradient)
                else Brush.horizontalGradient(listOf(BorderColor, BorderColor))
            )
            .clickable(enabled = enabled) {
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            color      = if (enabled) Color.White else TextMuted,
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── StatusChip ──────────────────────────────────────────────────────────────

enum class ChipStatus { ACTIVE, WARNING, CRITICAL, NEUTRAL }

@Composable
fun StatusChip(
    label: String,
    status: ChipStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        ChipStatus.ACTIVE   -> Color(0xFF10B981).copy(alpha = 0.15f) to Color(0xFF10B981)
        ChipStatus.WARNING  -> Color(0xFFF59E0B).copy(alpha = 0.15f) to Color(0xFFF59E0B)
        ChipStatus.CRITICAL -> Color(0xFFEF4444).copy(alpha = 0.15f) to Color(0xFFEF4444)
        ChipStatus.NEUTRAL  -> BorderColor to TextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── SectionHeader ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier            = modifier.fillMaxWidth(),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text       = title,
            color      = TextPrimary,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        action?.invoke()
    }
}

// ─── SecurityScoreRing ────────────────────────────────────────────────────────

@Composable
fun SecurityScoreRing(
    score: Int, // 0-100
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val scoreColor = when {
        score >= 80 -> ColorSuccess
        score >= 50 -> ColorWarning
        else        -> ColorError
    }

    Box(
        modifier        = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress         = { score / 100f },
            modifier         = Modifier.fillMaxSize(),
            color            = scoreColor,
            strokeWidth      = 6.dp,
            trackColor       = BorderColor
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = score.toString(),
                color      = TextPrimary,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = "/ 100", color = TextMuted, fontSize = 10.sp)
        }
    }
}

// ─── LoadingDots ──────────────────────────────────────────────────────────────

@Composable
fun LoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue  = 0.3f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentPurple.copy(alpha = alpha))
            )
        }
    }
}
