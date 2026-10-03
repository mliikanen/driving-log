package com.mikonoma.drivinglog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The light Petroleum scheme. Every role is set, so none of Material's default purple is left. */
private val LightColors = lightColorScheme(
    primary = OilSlickBlue,
    onPrimary = Color.White,
    primaryContainer = PetroleumMist,
    onPrimaryContainer = PetroleumDeep,
    inversePrimary = TealTint,
    secondary = RefineryTeal,
    onSecondary = Color.White,
    secondaryContainer = PetroleumMistSecondary,
    onSecondaryContainer = PetroleumDeep,
    tertiary = FuelGaugeGold,
    onTertiary = AsphaltDark,
    tertiaryContainer = GoldContainerLight,
    onTertiaryContainer = GoldOnContainerLight,
    background = CoolPlatinum,
    onBackground = AsphaltText,
    surface = CoolPlatinum,
    onSurface = AsphaltText,
    surfaceVariant = ExhaustFog,
    onSurfaceVariant = LightVariantText,
    surfaceTint = OilSlickBlue,
    inverseSurface = Color(0xFF202B30),
    inverseOnSurface = PlatinumText,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorOnContainerLight,
    outline = LightOutline,
    outlineVariant = ExhaustFog,
    scrim = Color.Black,
    surfaceBright = CoolPlatinum,
    surfaceDim = Color(0xFFD5DBDC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEFF3F3),
    surfaceContainer = Color(0xFFE8EDEE),
    surfaceContainerHigh = ExhaustFog,
    surfaceContainerHighest = Color(0xFFD9E0E5),
    primaryFixed = PetroleumMist,
    primaryFixedDim = TealTint,
    onPrimaryFixed = PetroleumDeep,
    onPrimaryFixedVariant = OilSlickBlue,
    secondaryFixed = PetroleumMistSecondary,
    secondaryFixedDim = TealTintLight,
    onSecondaryFixed = PetroleumDeep,
    onSecondaryFixedVariant = RefineryTeal,
    tertiaryFixed = GoldContainerLight,
    tertiaryFixedDim = FuelGaugeGold,
    onTertiaryFixed = GoldOnContainerLight,
    onTertiaryFixedVariant = GoldContainerDark,
)

/** The dark Petroleum scheme: Asphalt Dark, with tints of the blue-greens as the primary and secondary and the palette's own colors as their containers. */
private val DarkColors = darkColorScheme(
    primary = TealTint,
    onPrimary = PetroleumDeep,
    primaryContainer = OilSlickBlue,
    onPrimaryContainer = PetroleumMistOn,
    inversePrimary = OilSlickBlue,
    secondary = TealTintLight,
    onSecondary = PetroleumDeep,
    secondaryContainer = RefineryTeal,
    onSecondaryContainer = PetroleumMistSecondaryOn,
    tertiary = FuelGaugeGold,
    onTertiary = AsphaltDark,
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = GoldContainerLight,
    background = AsphaltDark,
    onBackground = PlatinumText,
    surface = AsphaltDark,
    onSurface = PlatinumText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkVariantText,
    surfaceTint = TealTint,
    inverseSurface = PlatinumText,
    inverseOnSurface = Color(0xFF263238),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Color.Black,
    surfaceBright = Color(0xFF2F3B42),
    surfaceDim = AsphaltDark,
    surfaceContainerLowest = Color(0xFF0D1215),
    surfaceContainerLow = Color(0xFF161E22),
    surfaceContainer = Color(0xFF1A2328),
    surfaceContainerHigh = Color(0xFF222D33),
    surfaceContainerHighest = Color(0xFF2A363D),
    primaryFixed = PetroleumMist,
    primaryFixedDim = TealTint,
    onPrimaryFixed = PetroleumDeep,
    onPrimaryFixedVariant = OilSlickBlue,
    secondaryFixed = PetroleumMistSecondary,
    secondaryFixedDim = TealTintLight,
    onSecondaryFixed = PetroleumDeep,
    onSecondaryFixedVariant = RefineryTeal,
    tertiaryFixed = GoldContainerLight,
    tertiaryFixedDim = FuelGaugeGold,
    onTertiaryFixed = GoldOnContainerLight,
    onTertiaryFixedVariant = GoldContainerDark,
)

/**
 * The colors of the app's own concepts, which Material's roles have no place for: [distance] draws a logged distance, [fuel] marks fuel and
 * refueling (not used by any screen yet), with [onFuel] as the text on a gold fill. Read them as `DrivingLogTheme.domain`; never hard-code them.
 */
@Immutable
data class DomainColors(val distance: Color, val fuel: Color, val onFuel: Color)

/** Road Trip Emerald is the distance color in the dark scheme; in the light scheme it is too pale to read as text, so a deeper shade is used. */
fun domainColors(darkTheme: Boolean): DomainColors = DomainColors(
    distance = if (darkTheme) RoadTripEmerald else EmeraldDeep,
    fuel = FuelGaugeGold,
    onFuel = AsphaltDark,
)

private val LocalDomainColors = staticCompositionLocalOf { domainColors(darkTheme = false) }
private val LocalDarkTheme = staticCompositionLocalOf { false }

object DrivingLogTheme {
    /** The distance and fuel colors of the current theme. */
    val domain: DomainColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDomainColors.current

    /** Whether the dark scheme is in use. */
    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalDarkTheme.current
}

/** The only place the app's color scheme is built: a pure function of the theme mode. */
fun drivingLogColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) DarkColors else LightColors

@Composable
fun DrivingLogTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDomainColors provides domainColors(darkTheme), LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = drivingLogColorScheme(darkTheme),
            content = content,
        )
    }
}
