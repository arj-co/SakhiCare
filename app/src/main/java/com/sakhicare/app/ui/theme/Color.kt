package com.sakhicare.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─── Modern Premium Palette ───
// Inspired by Apple Health / Flo / modern maternal health apps

// Primary — Warm Rose & Coral (maternal, warm, approachable)
val Primary = Color(0xFFE8647C)
val PrimaryDark = Color(0xFFD14D66)
val PrimaryLight = Color(0xFFFFF0F3)
val PrimaryMuted = Color(0xFFF9D5DC)

// Surface & Background — Clean Whites with warm undertone
val SurfaceWhite = Color(0xFFFFFFFF)
val BackgroundSoft = Color(0xFFFAF8F9)
val CardSurface = Color(0xFFFFFFFF)

// Triage Colors — Vibrant but refined
val TriageRed = Color(0xFFEF4444)
val TriageRedBg = Color(0xFFFEE2E2)
val TriageRedDark = Color(0xFFB91C1C)

val TriageAmber = Color(0xFFF59E0B)
val TriageAmberBg = Color(0xFFFEF3C7)
val TriageAmberDark = Color(0xFF92400E)

val TriageGreen = Color(0xFF10B981)
val TriageGreenBg = Color(0xFFD1FAE5)
val TriageGreenDark = Color(0xFF065F46)

// Neutrals — Modern grays
val Neutral900 = Color(0xFF111827)
val Neutral800 = Color(0xFF1F2937)
val Neutral700 = Color(0xFF374151)
val Neutral600 = Color(0xFF4B5563)
val Neutral500 = Color(0xFF6B7280)
val Neutral400 = Color(0xFF9CA3AF)
val Neutral300 = Color(0xFFD1D5DB)
val Neutral200 = Color(0xFFE5E7EB)
val Neutral100 = Color(0xFFF3F4F6)
val Neutral50 = Color(0xFFF9FAFB)

// Accent — Deep Indigo for interactive states
val AccentIndigo = Color(0xFF6366F1)
val AccentIndigoBg = Color(0xFFEEF2FF)

// Gradients
val HeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFE8647C), Color(0xFFF472B6), Color(0xFFEC4899))
)

val SubtleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFDF2F8), Color(0xFFFFF1F2))
)

// Legacy compat aliases (so existing code doesn't break)
val TealPrimary = Primary
val TealDark = PrimaryDark
val TealLight = PrimaryLight
val TealAccent = Primary
val TealBackground = BackgroundSoft
val TealGradientBrush = HeroGradient
val RedDanger = TriageRed
val RedDangerDark = TriageRedDark
val RedDangerContainer = TriageRedBg
val AmberWarning = TriageAmber
val AmberWarningDark = TriageAmberDark
val AmberWarningContainer = TriageAmberBg
val GreenSuccess = TriageGreen
val GreenSuccessDark = TriageGreenDark
val GreenSuccessContainer = TriageGreenBg
val TextDark = Neutral900
val TextSecondary = Neutral500
val CardBorder = Neutral200
val OfflineGrey = Neutral500
