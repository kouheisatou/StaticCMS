package io.github.kouheisatou.static_cms.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Windows 95 Color Palette
object RetroColors {
    val Background = Color(0xFFC0C0C0)
    val WindowBackground = Color(0xFFC0C0C0)
    val ButtonFace = Color(0xFFC0C0C0)
    val ButtonShadow = Color(0xFF808080)
    val ButtonDarkShadow = Color(0xFF000000)
    val ButtonHighlight = Color(0xFFDFDFDF)
    val ButtonLight = Color(0xFFFFFFFF)
    val ActiveBorder = Color(0xFF808080)
    val InactiveBorder = Color(0xFF808080)
    val WindowFrame = Color(0xFF000000)
    val WindowText = Color(0xFF000000)
    val TitleBarActive = Color(0xFF000080)
    val TitleBarInactive = Color(0xFF808080)
    val TitleBarText = Color(0xFFFFFFFF)
    val MenuText = Color(0xFF000000)
    val SelectedText = Color(0xFFFFFFFF)
    val SelectedBackground = Color(0xFF000080)
    val InfoText = Color(0xFF000000)
    val InfoBackground = Color(0xFFFFFFE1)
    val DisabledText = Color(0xFF808080)
}

object RetroTypography {
    val Default = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = RetroColors.WindowText
    )
    
    val Title = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = RetroColors.TitleBarText
    )
    
    val Button = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = RetroColors.WindowText
    )
}

@Composable
fun RetroTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ColorScheme(
        primary = RetroColors.TitleBarActive,
        onPrimary = RetroColors.TitleBarText,
        primaryContainer = RetroColors.ButtonFace,
        onPrimaryContainer = RetroColors.WindowText,
        inversePrimary = RetroColors.ButtonHighlight,
        secondary = RetroColors.ButtonFace,
        onSecondary = RetroColors.WindowText,
        secondaryContainer = RetroColors.ButtonFace,
        onSecondaryContainer = RetroColors.WindowText,
        tertiary = RetroColors.ButtonFace,
        onTertiary = RetroColors.WindowText,
        tertiaryContainer = RetroColors.ButtonFace,
        onTertiaryContainer = RetroColors.WindowText,
        background = RetroColors.Background,
        onBackground = RetroColors.WindowText,
        surface = RetroColors.WindowBackground,
        onSurface = RetroColors.WindowText,
        surfaceVariant = RetroColors.ButtonFace,
        onSurfaceVariant = RetroColors.WindowText,
        surfaceTint = RetroColors.TitleBarActive,
        inverseSurface = RetroColors.SelectedBackground,
        inverseOnSurface = RetroColors.SelectedText,
        error = Color.Red,
        onError = Color.White,
        errorContainer = Color.Red.copy(alpha = 0.1f),
        onErrorContainer = Color.Red,
        outline = RetroColors.ActiveBorder,
        outlineVariant = RetroColors.InactiveBorder,
        scrim = Color.Black.copy(alpha = 0.32f),
        surfaceBright = RetroColors.ButtonLight,
        surfaceDim = RetroColors.ButtonShadow,
        surfaceContainer = RetroColors.ButtonFace,
        surfaceContainerHigh = RetroColors.ButtonFace,
        surfaceContainerHighest = RetroColors.ButtonFace,
        surfaceContainerLow = RetroColors.ButtonFace,
        surfaceContainerLowest = RetroColors.ButtonFace,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
} 