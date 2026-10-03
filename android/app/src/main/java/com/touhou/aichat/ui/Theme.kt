package com.touhou.aichat.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touhou.aichat.R

val WinBlue = Color(0xFF2AABEE)          // Telegram blue
val WinLightGray = Color(0xFFF3F3F3)
val WinSurface = Color(0xFFFFFFFF)
val WinTextDark = Color(0xFF1A1A1A)
val WinTextGray = Color(0xFF8A8A8A)

val GlassWhite = Color(0xFFFFFFFF)        // solid white (top bar / nav)
val GlassWhiteSoft = Color(0xFFEFF3F6)    // light gray (incoming bubble / cards)
val GlassBlue = Color(0xFF2AABEE)         // Telegram blue (outgoing bubble)
val WallpaperScrim = Color(0x26FFFFFF)

private val LightColors = lightColorScheme(
    primary = WinBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EFFC),
    onPrimaryContainer = Color(0xFF004C6D),
    secondary = Color(0xFF2AABEE),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F4FF),
    onSecondaryContainer = Color(0xFF004C6D),
    background = Color.White,
    onBackground = WinTextDark,
    surface = Color.White,
    onSurface = WinTextDark,
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = WinTextGray,
    outline = Color(0xFF8A8A8A),
    error = Color(0xFFC42B1C),
    onError = Color.White
)

private val WinShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

val AppFontFamily = FontFamily(Font(R.font.msyh, FontWeight.Normal))

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp)
)

@Composable
fun TouhouTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = WinShapes,
        typography = AppTypography,
        content = content
    )
}
