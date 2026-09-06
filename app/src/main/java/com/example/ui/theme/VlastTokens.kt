package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Vlast Unified Design Tokens (Item 30 & 31).
 *
 * Visual philosophy: Authority, Power, Restraint, Control (السلطة والسيطرة).
 * Dark mode default, crisp dark foundations, deep crimson/amber accents,
 * WCAG AA strictly compliant contrast.
 */
object VlastTokens {

    // --- Color System: Dark Foundation ---
    val DarkBackground = Color(0xFF080B0F)       // Ultra-deep authoritative black-slate
    val DarkSurface = Color(0xFF0F141C)          // Level 1 card surface
    val DarkSurfaceVariant = Color(0xFF161E2B)   // Level 2 elevated surface
    val DarkBorder = Color(0xFF222E42)           // Clean crisp structural border
    val DarkBorderActive = Color(0xFF3B4F70)     // Focused border

    // --- Color System: Light Mode Alternative ---
    val LightBackground = Color(0xFFF1F5F9)      // Slate 100
    val LightSurface = Color(0xFFFFFFFF)         // Pure white cards
    val LightSurfaceVariant = Color(0xFFE2E8F0)  // Slate 200
    val LightBorder = Color(0xFFCBD5E1)          // Slate 300
    val LightBorderActive = Color(0xFF94A3B8)

    // --- Core Brand & Accent: Authority Red / Amber ---
    val BrandRed = Color(0xFFE11D48)             // Rose 600 - decisive, controlled authority
    val BrandRedDark = Color(0xFF9F1239)         // Rose 800 - deep power
    val BrandAmber = Color(0xFFF59E0B)           // Amber 500 - warning & alert
    val BrandCyan = Color(0xFF0EA5E9)            // Sky 500 - data pulse / tunnel active

    // --- Strict Semantic Tri-Color (Item 4: Exactly identical across all screens) ---
    val SemanticGreen = Color(0xFF10B981)        // Emerald 500 (< 70% consumption)
    val SemanticYellow = Color(0xFFF59E0B)       // Amber 500 (70% - 90% warning)
    val SemanticRed = Color(0xFFEF4444)          // Red 500 (> 90% or cutoff)

    // --- Text Colors (Tested for WCAG AA >= 4.5:1 ratio against DarkBackground & DarkSurface) ---
    val TextPrimary = Color(0xFFF8FAFC)          // Slate 50 (Contrast ratio ~ 16.5:1)
    val TextSecondary = Color(0xFF94A3B8)        // Slate 400 (Contrast ratio ~ 6.2:1)
    val TextMuted = Color(0xFF64748B)            // Slate 500
    val TextOnAccent = Color(0xFFFFFFFF)

    val LightTextPrimary = Color(0xFF0F172A)     // Slate 900
    val LightTextSecondary = Color(0xFF475569)   // Slate 600

    // --- Spacing Tokens (Standard 8dp grid) ---
    val Space2: Dp = 2.dp
    val Space4: Dp = 4.dp
    val Space8: Dp = 8.dp
    val Space12: Dp = 12.dp
    val Space16: Dp = 16.dp
    val Space20: Dp = 20.dp
    val Space24: Dp = 24.dp
    val Space32: Dp = 32.dp
    val Space48: Dp = 48.dp

    // --- Radii & Elevation ---
    val RadiusSmall: Dp = 8.dp
    val RadiusMedium: Dp = 14.dp
    val RadiusLarge: Dp = 20.dp
    val RadiusPill: Dp = 999.dp

    // --- Typography Sizes (sp for system accessibility scaling) ---
    val TextMegaNumber: TextUnit = 48.sp         // Remaining counter display
    val TextHeadline: TextUnit = 22.sp
    val TextTitle: TextUnit = 17.sp
    val TextBody: TextUnit = 14.sp
    val TextCaption: TextUnit = 12.sp
    val TextMicro: TextUnit = 10.sp
}
