package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CairoFontFamily = FontFamily(
  Font(R.font.cairo, FontWeight.Normal),
  Font(R.font.cairo, FontWeight.Medium),
  Font(R.font.cairo, FontWeight.SemiBold),
  Font(R.font.cairo, FontWeight.Bold),
)

val IbmPlexArabicFontFamily = FontFamily(
  Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
  Font(R.font.ibm_plex_sans_arabic, FontWeight.Medium),
  Font(R.font.ibm_plex_sans_arabic, FontWeight.SemiBold),
)

fun buildDronesTypography(scale: Float = 1.0f): Typography {
  val safeScale = scale.coerceIn(0.8f, 1.5f)
  return Typography(
    headlineMedium = TextStyle(
      fontFamily = CairoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (24 * safeScale).sp,
      lineHeight = (34 * safeScale).sp,
    ),
    titleLarge = TextStyle(
      fontFamily = CairoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (20 * safeScale).sp,
      lineHeight = (30 * safeScale).sp,
    ),
    titleMedium = TextStyle(
      fontFamily = CairoFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (17 * safeScale).sp,
      lineHeight = (25 * safeScale).sp,
    ),
    titleSmall = TextStyle(
      fontFamily = CairoFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (14 * safeScale).sp,
      lineHeight = (20 * safeScale).sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = IbmPlexArabicFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (16 * safeScale).sp,
      lineHeight = (26 * safeScale).sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = IbmPlexArabicFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (14 * safeScale).sp,
      lineHeight = (22 * safeScale).sp,
    ),
    bodySmall = TextStyle(
      fontFamily = IbmPlexArabicFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (12 * safeScale).sp,
      lineHeight = (18 * safeScale).sp,
    ),
    labelLarge = TextStyle(
      fontFamily = CairoFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (14 * safeScale).sp,
      lineHeight = (20 * safeScale).sp,
    ),
    labelMedium = TextStyle(
      fontFamily = CairoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = (12 * safeScale).sp,
      lineHeight = (16 * safeScale).sp,
    ),
  )
}

val Typography = buildDronesTypography(1.0f)
