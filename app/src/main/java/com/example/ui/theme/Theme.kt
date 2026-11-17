package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class SemanticStatusColors(
  val success: Color,
  val successContainer: Color,
  val error: Color,
  val errorContainer: Color,
)

val LocalSemanticStatusColors = staticCompositionLocalOf {
  SemanticStatusColors(
    success = StatusSuccessLight,
    successContainer = StatusSuccessContainerLight,
    error = StatusErrorLight,
    errorContainer = StatusErrorContainerLight,
  )
}

private val LightColorScheme = lightColorScheme(
  primary = AerospacePrimaryLight,
  onPrimary = AerospaceOnPrimaryLight,
  primaryContainer = AerospacePrimaryContainerLight,
  onPrimaryContainer = AerospaceOnPrimaryContainerLight,
  secondary = TealSecondaryLight,
  onSecondary = TealOnSecondaryLight,
  secondaryContainer = TealSecondaryContainerLight,
  onSecondaryContainer = TealOnSecondaryContainerLight,
  tertiary = AmberTertiaryLight,
  onTertiary = AmberOnTertiaryLight,
  tertiaryContainer = AmberTertiaryContainerLight,
  onTertiaryContainer = AmberOnTertiaryContainerLight,
  background = AerospaceBackgroundLight,
  surface = AerospaceSurfaceLight,
  surfaceVariant = AerospaceSurfaceVariantLight,
  onBackground = AerospaceOnSurfaceLight,
  onSurface = AerospaceOnSurfaceLight,
  onSurfaceVariant = AerospaceOnSurfaceVariantLight,
  outline = AerospaceOutlineLight,
)

private val DarkColorScheme = darkColorScheme(
  primary = AerospacePrimaryDark,
  onPrimary = AerospaceOnPrimaryDark,
  primaryContainer = AerospacePrimaryContainerDark,
  onPrimaryContainer = AerospaceOnPrimaryContainerDark,
  secondary = TealSecondaryDark,
  onSecondary = TealOnSecondaryDark,
  secondaryContainer = TealSecondaryContainerDark,
  onSecondaryContainer = TealOnSecondaryContainerDark,
  tertiary = AmberTertiaryDark,
  onTertiary = AmberOnTertiaryDark,
  tertiaryContainer = AmberTertiaryContainerDark,
  onTertiaryContainer = AmberOnTertiaryContainerDark,
  background = AerospaceBackgroundDark,
  surface = AerospaceSurfaceDark,
  surfaceVariant = AerospaceSurfaceVariantDark,
  onBackground = AerospaceOnSurfaceDark,
  onSurface = AerospaceOnSurfaceDark,
  onSurfaceVariant = AerospaceOnSurfaceVariantDark,
  outline = AerospaceOutlineDark,
)

private fun ColorScheme.applyHighContrast(darkTheme: Boolean): ColorScheme =
  if (darkTheme) {
    copy(
      background = Color(0xFF000000),
      surface = Color(0xFF0B1117),
      surfaceVariant = Color(0xFF1A2530),
      onBackground = Color(0xFFFFFFFF),
      onSurface = Color(0xFFFFFFFF),
      onSurfaceVariant = Color(0xFFE2ECF5),
      outline = Color(0xFF9AC2E6),
    )
  } else {
    copy(
      background = Color(0xFFFFFFFF),
      surface = Color(0xFFFFFFFF),
      surfaceVariant = Color(0xFFE6EEF5),
      onBackground = Color(0xFF000000),
      onSurface = Color(0xFF000000),
      onSurfaceVariant = Color(0xFF1E293B),
      outline = Color(0xFF1E3A5F),
    )
  }

@Composable
fun Drones3DTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  highContrast: Boolean = false,
  fontScale: Float = 1.0f,
  content: @Composable () -> Unit,
) {
  val baseScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val finalScheme = if (highContrast) baseScheme.applyHighContrast(darkTheme) else baseScheme
  val statusColors =
    if (darkTheme) {
      SemanticStatusColors(
        success = StatusSuccessDark,
        successContainer = StatusSuccessContainerDark,
        error = StatusErrorDark,
        errorContainer = StatusErrorContainerDark,
      )
    } else {
      SemanticStatusColors(
        success = StatusSuccessLight,
        successContainer = StatusSuccessContainerLight,
        error = StatusErrorLight,
        errorContainer = StatusErrorContainerLight,
      )
    }

  CompositionLocalProvider(LocalSemanticStatusColors provides statusColors) {
    MaterialTheme(
      colorScheme = finalScheme,
      typography = buildDronesTypography(fontScale),
      content = content,
    )
  }
}
