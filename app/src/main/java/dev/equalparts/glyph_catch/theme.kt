package dev.equalparts.glyph_catch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object CatchColors {
    val Red = Color(0xFFD71921)
    val Black = Color(0xFF000000)
    val DarkGray = Color(0xFF090909)
    val MediumGray = Color(0xFF1A1A1A)
    val LightGray = Color(0xFF888888)
    val VeryLightGray = Color(0xFFF5F5F5)
    val White = Color(0xFFFFFFFF)
    val OffWhite = Color(0xFFFAFAFA)
    val DarkText = Color(0xFF1A1A1A)
    val MediumText = Color(0xFF666666)
}

val ndotFontFamily = FontFamily(
    Font(R.font.ndot_47_inspired_by_nothing, FontWeight.Normal)
)

val darkColorScheme = darkColorScheme(
    primary = CatchColors.Red,
    onPrimary = CatchColors.White,
    primaryContainer = CatchColors.MediumGray,
    onPrimaryContainer = CatchColors.White,
    background = CatchColors.Black,
    surface = CatchColors.DarkGray,
    surfaceVariant = CatchColors.MediumGray,
    onBackground = CatchColors.White,
    onSurface = CatchColors.White,
    onSurfaceVariant = CatchColors.LightGray,
    outline = CatchColors.MediumGray,
    outlineVariant = CatchColors.DarkGray
)

val lightColorScheme = lightColorScheme(
    primary = CatchColors.Red,
    onPrimary = CatchColors.White,
    primaryContainer = CatchColors.VeryLightGray,
    onPrimaryContainer = CatchColors.DarkText,
    background = CatchColors.OffWhite,
    surface = CatchColors.White,
    surfaceVariant = CatchColors.VeryLightGray,
    onBackground = CatchColors.DarkText,
    onSurface = CatchColors.DarkText,
    onSurfaceVariant = CatchColors.MediumText,
    outline = CatchColors.LightGray.copy(alpha = 0.3f),
    outlineVariant = CatchColors.LightGray.copy(alpha = 0.1f)
)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme
    } else {
        lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

object AppSizes {
    val none = 0.dp

    val spacingMicro = 2.dp
    val spacingTiny = 4.dp
    val spacingExtraSmall = 6.dp
    val spacingSmall = 8.dp
    val spacingMedium = 12.dp
    val spacingLarge = 16.dp
    val spacingXLarge = 20.dp
    val spacingXXLarge = 24.dp

    val cardCornerRadius = 12.dp
    val chipCornerRadius = 4.dp

    val iconSizeTiny = 16.dp
    val iconSizeSmall = 18.dp
    val iconSizeMedium = 24.dp

    val pokemonImageSize = 70.dp

    val homeRecentCardHeight = 150.dp
    val homeTileHeight = 150.dp
    val homeWeatherImageSize = 60.dp

    val strokeWidthThin = 2.dp
}
