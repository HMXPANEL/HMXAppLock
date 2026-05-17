package com.hmx.shield.core.theme

import androidx.compose.ui.graphics.Color

// ─── Background ───────────────────────────────────────────────────────────────
val BgPrimary     = Color(0xFF0B0B0F)
val BgSecondary   = Color(0xFF13131A)
val CardBg        = Color(0xFF1A1B22)
val CardBgLight   = Color(0xFF1E1F28)
val BorderColor   = Color(0xFF2A2C35)
val SurfaceColor  = Color(0xFF16171E)

// ─── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB8BCC8)
val TextMuted     = Color(0xFF6B7080)
val TextHint      = Color(0x80B8BCC8)

// ─── Accent Palette ───────────────────────────────────────────────────────────
val AccentPurple  = Color(0xFF8B5CF6)
val AccentBlue    = Color(0xFF3B82F6)
val AccentCyan    = Color(0xFF06B6D4)
val AccentEmerald = Color(0xFF10B981)
val AccentRed     = Color(0xFFEF4444)
val AccentOrange  = Color(0xFFF97316)
val AccentAmber   = Color(0xFFF59E0B)

// ─── Semantic ─────────────────────────────────────────────────────────────────
val ColorSuccess  = Color(0xFF10B981)
val ColorWarning  = Color(0xFFF59E0B)
val ColorError    = Color(0xFFEF4444)
val ColorInfo     = Color(0xFF3B82F6)

// ─── Gradients (used as Brush.linearGradient) ─────────────────────────────────
val GradientPurpleBlue    = listOf(AccentPurple, AccentBlue)
val GradientRedOrange     = listOf(AccentRed, AccentOrange)
val GradientCyanEmerald   = listOf(AccentCyan, AccentEmerald)
val GradientBgVertical    = listOf(BgPrimary, BgSecondary)

// ─── Overlay (lock screen specific) ──────────────────────────────────────────
val OverlayBgTop    = Color(0xFF060611)
val OverlayBgBottom = Color(0xFF0D1B2A)
val PinDotFill      = AccentPurple
val PinDotEmpty     = Color(0xFF2A2A3E)
val NumKeyBg        = Color(0xFF1A1A2E)
