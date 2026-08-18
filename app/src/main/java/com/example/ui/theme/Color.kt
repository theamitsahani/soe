package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core brand colors
val Navy900 = Color(0xFF0F172A)
val Navy800 = Color(0xFF1E293B)
val Navy950 = Color(0xFF090D16)
val Indigo600 = Color(0xFF4F46E5)
val Indigo700 = Color(0xFF4338CA)
val Indigo500 = Color(0xFF6366F1)
val Indigo400 = Color(0xFF818CF8)
val Indigo50 = Color(0xFFEEF2FF)
val Teal600 = Color(0xFF0D9488)
val Teal500 = Color(0xFF14B8A6)
val Teal50 = Color(0xFFF0FDFA)
val Cyan500 = Color(0xFF06B6D4)
val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald50 = Color(0xFFECFDF5)
val Amber600 = Color(0xFFD97706)
val Amber500 = Color(0xFFF59E0B)
val Amber100 = Color(0xFFFEF3C7)
val Amber50 = Color(0xFFFFFBEB)
val Rose600 = Color(0xFFE11D48)
val Rose500 = Color(0xFFF43F5E)
val Purple600 = Color(0xFF9333EA)
val Purple500 = Color(0xFFA855F7)
val Sky500 = Color(0xFF0EA5E9)
val Blue600 = Color(0xFF2563EB)

// Slate Neutrals
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
val Red600 = Color(0xFFDC2626)
val Red500 = Color(0xFFEF4444)
val Red100 = Color(0xFFFEE2E2)
val Red50 = Color(0xFFFEF2F2)

// Liquid Glass Surfaces & Highlights
val GlassSurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.72f)
val GlassSurfaceLightElevated = Color(0xFFFFFFFF).copy(alpha = 0.88f)
val GlassSurfaceLightSubtle = Color(0xFFFFFFFF).copy(alpha = 0.45f)
val GlassBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.80f)
val GlassBorderLightSubtle = Color(0xFFCBD5E1).copy(alpha = 0.40f)

val GlassSurfaceDark = Color(0xFF1E293B).copy(alpha = 0.75f)
val GlassSurfaceDarkElevated = Color(0xFF334155).copy(alpha = 0.85f)
val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.15f)

// Glass Gradients
val GlassIndigoGradient = Brush.linearGradient(
    colors = listOf(Indigo600, Color(0xFF7C3AED))
)
val GlassTealGradient = Brush.linearGradient(
    colors = listOf(Teal600, Cyan500)
)
val GlassAmberGradient = Brush.linearGradient(
    colors = listOf(Amber500, Color(0xFFEA580C))
)
val GlassEmeraldGradient = Brush.linearGradient(
    colors = listOf(Emerald600, Teal500)
)
val GlassSurfaceGradient = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.90f),
        Color.White.copy(alpha = 0.70f)
    )
)
val GlassCardBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.95f),
        Color.White.copy(alpha = 0.35f),
        Indigo400.copy(alpha = 0.25f)
    )
)

