package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThemeMode

data class NovaExtendedColors(
    val isDark: Boolean,
    val orbGradient: Brush,
    val glassSurface: Color,
    val glassBorder: Brush,
    val cyanAccent: Color,
    val violetAccent: Color,
    val magentaAccent: Color,
    val healthGreen: Color,
    val warningAmber: Color,
    val errorRed: Color
)

val LocalNovaColors = staticCompositionLocalOf {
    NovaExtendedColors(
        isDark = true,
        orbGradient = Brush.linearGradient(listOf(NovaCyan, NovaViolet, NovaMagenta)),
        glassSurface = NovaGlassDark.copy(alpha = 0.78f),
        glassBorder = Brush.linearGradient(
            listOf(
                NovaCyan.copy(alpha = 0.45f),
                NovaViolet.copy(alpha = 0.35f),
                NovaMagenta.copy(alpha = 0.35f)
            )
        ),
        cyanAccent = NovaCyan,
        violetAccent = NovaViolet,
        magentaAccent = NovaMagenta,
        healthGreen = NovaEmerald,
        warningAmber = NovaAmber,
        errorRed = NovaCrimson
    )
}

private val NovaDarkColorScheme = darkColorScheme(
    primary = NovaCyan,
    onPrimary = NovaOledBlack,
    primaryContainer = NovaGlassElevated,
    onPrimaryContainer = NovaCyan,
    secondary = NovaViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF21174A),
    onSecondaryContainer = Color(0xFFE0D4FF),
    tertiary = NovaMagenta,
    onTertiary = Color.White,
    background = NovaOledBlack,
    onBackground = NovaTextPrimaryDark,
    surface = NovaObsidian,
    onSurface = NovaTextPrimaryDark,
    surfaceVariant = NovaGlassDark,
    onSurfaceVariant = NovaTextSecondaryDark,
    outline = NovaBorderDark,
    error = NovaCrimson,
    onError = Color.White
)

private val NovaLightColorScheme = lightColorScheme(
    primary = NovaCyanDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F7FF),
    onPrimaryContainer = Color(0xFF003644),
    secondary = NovaVioletDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = NovaMagentaDeep,
    onTertiary = Color.White,
    background = NovaLightCanvas,
    onBackground = NovaTextPrimaryLight,
    surface = NovaLightSurface,
    onSurface = NovaTextPrimaryLight,
    surfaceVariant = NovaLightElevated,
    onSurfaceVariant = NovaTextSecondaryLight,
    outline = NovaBorderLight,
    error = NovaCrimson,
    onError = Color.White
)

val NovaShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NovaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accessibilityLargeText: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> systemDark
    }

    val colorScheme = if (useDark) NovaDarkColorScheme else NovaLightColorScheme
    val extendedColors = if (useDark) {
        NovaExtendedColors(
            isDark = true,
            orbGradient = Brush.linearGradient(listOf(NovaCyan, NovaViolet, NovaMagenta)),
            glassSurface = NovaGlassDark.copy(alpha = 0.80f),
            glassBorder = Brush.linearGradient(
                listOf(
                    NovaCyan.copy(alpha = 0.50f),
                    NovaViolet.copy(alpha = 0.35f),
                    NovaMagenta.copy(alpha = 0.40f)
                )
            ),
            cyanAccent = NovaCyan,
            violetAccent = NovaViolet,
            magentaAccent = NovaMagenta,
            healthGreen = NovaEmerald,
            warningAmber = NovaAmber,
            errorRed = NovaCrimson
        )
    } else {
        NovaExtendedColors(
            isDark = false,
            orbGradient = Brush.linearGradient(listOf(NovaCyanDeep, NovaVioletDeep, NovaMagentaDeep)),
            glassSurface = NovaLightSurface.copy(alpha = 0.90f),
            glassBorder = Brush.linearGradient(
                listOf(
                    NovaCyanDeep.copy(alpha = 0.35f),
                    NovaVioletDeep.copy(alpha = 0.30f),
                    NovaMagentaDeep.copy(alpha = 0.30f)
                )
            ),
            cyanAccent = NovaCyanDeep,
            violetAccent = NovaVioletDeep,
            magentaAccent = NovaMagentaDeep,
            healthGreen = NovaEmerald,
            warningAmber = NovaAmber,
            errorRed = NovaCrimson
        )
    }

    val typography = if (accessibilityLargeText) {
        NovaTypography.copy(
            bodyLarge = NovaTypography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
            bodyMedium = NovaTypography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
            titleLarge = NovaTypography.titleLarge.copy(fontSize = 21.sp, lineHeight = 28.sp),
            labelLarge = NovaTypography.labelLarge.copy(fontSize = 15.sp)
        )
    } else {
        NovaTypography
    }

    CompositionLocalProvider(LocalNovaColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = NovaShapes,
            content = content
        )
    }
}
