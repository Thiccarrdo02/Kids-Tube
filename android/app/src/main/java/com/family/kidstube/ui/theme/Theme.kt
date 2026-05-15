package com.family.kidstube.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BrandRed = Color(0xFFFF0000)
val NearBlack = Color(0xFF0F0F0F)
val SubtleGray = Color(0xFF606060)
val Divider = Color(0xFFE5E5E5)

private val LightColors = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    secondary = NearBlack,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = NearBlack,
    surface = Color.White,
    onSurface = NearBlack,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = SubtleGray,
)

// Roboto is the system default on Android -- referencing FontFamily.Default
// gives Roboto on Android without bundling fonts.
private val Roboto = FontFamily.Default

private val AppTypography = Typography(
    titleMedium = TextStyle(
        fontFamily = Roboto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Roboto, fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Roboto, fontSize = 12.sp, color = SubtleGray
    ),
    labelSmall = TextStyle(
        fontFamily = Roboto, fontSize = 11.sp
    ),
)

@Composable
fun KidsTubeTheme(content: @Composable () -> Unit) {
    // Light mode only -- matches the spec (white background).
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
