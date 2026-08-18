package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Base Neutrals & Dark
val Navy950 = Color(0xFF0A0F1D)
val Navy900 = Color(0xFF0F172A)
val Navy800 = Color(0xFF1E293B)
val Navy700 = Color(0xFF334155)

// Slates
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

// Primary Brand & Accents
val Indigo500 = Color(0xFF6366F1)
val Indigo600 = Color(0xFF4F46E5)
val Indigo700 = Color(0xFF4338CA)
val Violet500 = Color(0xFF8B5CF6)
val Violet600 = Color(0xFF7C3AED)
val Cyan500 = Color(0xFF06B6D4)
val Cyan600 = Color(0xFF0891B2)
val Teal500 = Color(0xFF14B8A6)
val Teal600 = Color(0xFF0D9488)

// Semantic Accents
val Emerald500 = Color(0xFF10B981)
val Emerald600 = Color(0xFF059669)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald50 = Color(0xFFECFDF5)

val Amber500 = Color(0xFFF59E0B)
val Amber600 = Color(0xFFD97706)
val Amber100 = Color(0xFFFEF3C7)
val Amber50 = Color(0xFFFFFBEB)

val Red500 = Color(0xFFEF4444)
val Red600 = Color(0xFFDC2626)
val Red100 = Color(0xFFFEE2E2)
val Red50 = Color(0xFFFEF2F2)

// Liquid Glassmorphism Surfaces & Tints
val GlassSurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.82f)
val GlassSurfaceElevated = Color(0xFFFFFFFF).copy(alpha = 0.92f)
val GlassSurfaceSubtle = Color(0xFFFFFFFF).copy(alpha = 0.65f)
val GlassSurfaceUltraLight = Color(0xFFFFFFFF).copy(alpha = 0.45f)

val GlassSurfaceDark = Color(0xFF1E293B).copy(alpha = 0.82f)
val GlassSurfaceDarkSubtle = Color(0xFF0F172A).copy(alpha = 0.70f)

// Glass Borders & Highlights
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.75f)
val GlassBorderSubtle = Color(0xFFE2E8F0).copy(alpha = 0.65f)
val GlassBorderDark = Color(0xFF334155).copy(alpha = 0.65f)
val GlassHighlightTop = Color(0xFFFFFFFF).copy(alpha = 0.95f)

// Liquid Glows
val GlowPrimary = Color(0xFF6366F1).copy(alpha = 0.18f)
val GlowTeal = Color(0xFF14B8A6).copy(alpha = 0.15f)
val GlowViolet = Color(0xFF8B5CF6).copy(alpha = 0.14f)
val GlowEmerald = Color(0xFF10B981).copy(alpha = 0.16f)
val GlowAmber = Color(0xFFF59E0B).copy(alpha = 0.16f)
val GlowRose = Color(0xFFF43F5E).copy(alpha = 0.16f)

// Gradients
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
)

val LiquidHeaderGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
)

val SuccessGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF059669), Color(0xFF10B981))
)

val AccentGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF6366F1), Color(0xFF06B6D4))
)
