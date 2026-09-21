package com.mikonoma.drivinglog.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ThemeColorsTest {

    private val light = drivingLogColorScheme(darkTheme = false)
    private val dark = drivingLogColorScheme(darkTheme = true)

    private class Pair(val name: String, val foreground: (ColorScheme) -> Color, val background: (ColorScheme) -> Color, val minimum: Double)

    /** Every text on its background needs 4.5:1. */
    private val textPairs = listOf(
        Pair("onBackground on background", { it.onBackground }, { it.background }, 4.5),
        Pair("onSurface on surface", { it.onSurface }, { it.surface }, 4.5),
        Pair("onSurfaceVariant on surfaceVariant", { it.onSurfaceVariant }, { it.surfaceVariant }, 4.5),
        Pair("onSurfaceVariant on background", { it.onSurfaceVariant }, { it.background }, 4.5),
        Pair("onSurface on surfaceContainer", { it.onSurface }, { it.surfaceContainer }, 4.5),
        Pair("onSurface on surfaceContainerHighest", { it.onSurface }, { it.surfaceContainerHighest }, 4.5),
        Pair("onPrimary on primary", { it.onPrimary }, { it.primary }, 4.5),
        Pair("onPrimaryContainer on primaryContainer", { it.onPrimaryContainer }, { it.primaryContainer }, 4.5),
        Pair("onSecondary on secondary", { it.onSecondary }, { it.secondary }, 4.5),
        Pair("onSecondaryContainer on secondaryContainer", { it.onSecondaryContainer }, { it.secondaryContainer }, 4.5),
        Pair("onTertiary on tertiary", { it.onTertiary }, { it.tertiary }, 4.5),
        Pair("onTertiaryContainer on tertiaryContainer", { it.onTertiaryContainer }, { it.tertiaryContainer }, 4.5),
        Pair("onError on error", { it.onError }, { it.error }, 4.5),
        Pair("onErrorContainer on errorContainer", { it.onErrorContainer }, { it.errorContainer }, 4.5),
        Pair("error text on background", { it.error }, { it.background }, 4.5),
        Pair("inverseOnSurface on inverseSurface", { it.inverseOnSurface }, { it.inverseSurface }, 4.5),
        Pair("primary (a text button) on background", { it.primary }, { it.background }, 4.5),
    )

    /** Every outline, icon and control needs 3:1 against what it sits on. */
    private val controlPairs = listOf(
        Pair("outline on background", { it.outline }, { it.background }, 3.0),
        Pair("outline on surface", { it.outline }, { it.surface }, 3.0),
        Pair("primary on background", { it.primary }, { it.background }, 3.0),
        Pair("secondary on background", { it.secondary }, { it.background }, 3.0),
        Pair("error on background", { it.error }, { it.background }, 3.0),
    )

    private fun assertAll(scheme: ColorScheme, mode: String, pairs: List<Pair>) {
        for (pair in pairs) {
            val ratio = contrastRatio(pair.foreground(scheme), pair.background(scheme))
            assertTrue(ratio >= pair.minimum, "$mode: ${pair.name} is $ratio:1, needs ${pair.minimum}:1")
        }
    }

    // ---- The contrast helper

    @Test
    fun blackOnWhiteIs21To1AndAColorOnItselfIs1To1() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(CoolPlatinum, CoolPlatinum), 0.0001)
    }

    @Test
    fun theRatioDoesNotDependOnTheOrder() {
        assertEquals(contrastRatio(OilSlickBlue, CoolPlatinum), contrastRatio(CoolPlatinum, OilSlickBlue), 0.0000001)
    }

    @Test
    fun knownPairsMatchTheDesignsFigures() {
        assertEquals(16.72, contrastRatio(Color.White, PetroleumDeep), 0.05)
        assertEquals(12.02, contrastRatio(Color.White, OilSlickBlue), 0.05)
        assertEquals(16.62, contrastRatio(AsphaltDark, CoolPlatinum), 0.05)
    }

    @Test
    fun theRawPalettePairsTheDesignReplacesReallyAreTooLow() {
        // The reason the derived colors exist: these would fail the rule if used as they are.
        assertTrue(contrastRatio(RoadTripEmerald, CoolPlatinum) < 3.0)
        assertTrue(contrastRatio(FuelGaugeGold, CoolPlatinum) < 3.0)
        assertTrue(contrastRatio(OilSlickBlue, AsphaltDark) < 3.0)
        assertTrue(contrastRatio(RefineryTeal, AsphaltDark) < 3.0)
    }

    // ---- Palette colors are in their roles

    @Test
    fun theLightSchemeUsesThePaletteWhereTheDocumentSaysSo() {
        assertEquals(CoolPlatinum, light.background)
        assertEquals(CoolPlatinum, light.surface)
        assertEquals(OilSlickBlue, light.primary)
        assertEquals(Color.White, light.onPrimary)
        assertEquals(RefineryTeal, light.secondary)
        assertEquals(ExhaustFog, light.outlineVariant)
        assertEquals(ExhaustFog, light.surfaceVariant)
        assertEquals(FuelGaugeGold, light.tertiary)
    }

    @Test
    fun theDarkSchemeUsesAsphaltDarkAndTheBlueGreensAsContainers() {
        assertEquals(AsphaltDark, dark.background)
        assertEquals(AsphaltDark, dark.surface)
        assertEquals(OilSlickBlue, dark.primaryContainer)
        assertEquals(RefineryTeal, dark.secondaryContainer)
        assertEquals(FuelGaugeGold, dark.tertiary)
    }

    @Test
    fun thePaletteColorsAreTheDocumentsHexValues() {
        assertEquals(Color(0xFF0F2027), PetroleumDeep)
        assertEquals(Color(0xFF203A43), OilSlickBlue)
        assertEquals(Color(0xFF2C5364), RefineryTeal)
        assertEquals(Color(0xFFFFB703), FuelGaugeGold)
        assertEquals(Color(0xFF06D6A0), RoadTripEmerald)
        assertEquals(Color(0xFF12181B), AsphaltDark)
        assertEquals(Color(0xFFF4F7F6), CoolPlatinum)
        assertEquals(Color(0xFFE0E6ED), ExhaustFog)
    }

    // ---- Nothing of Material's default is left

    @Test
    fun noMainRoleIsMaterialsDefaultPurple() {
        for ((mode, scheme, default) in listOf(Triple("light", light, lightColorScheme()), Triple("dark", dark, darkColorScheme()))) {
            assertNotEquals(default.primary, scheme.primary, "$mode primary")
            assertNotEquals(default.secondary, scheme.secondary, "$mode secondary")
            assertNotEquals(default.tertiary, scheme.tertiary, "$mode tertiary")
            assertNotEquals(default.primaryContainer, scheme.primaryContainer, "$mode primaryContainer")
            assertNotEquals(default.secondaryContainer, scheme.secondaryContainer, "$mode secondaryContainer")
            assertNotEquals(default.tertiaryContainer, scheme.tertiaryContainer, "$mode tertiaryContainer")
            assertNotEquals(default.background, scheme.background, "$mode background")
            assertNotEquals(default.surface, scheme.surface, "$mode surface")
            assertNotEquals(default.surfaceVariant, scheme.surfaceVariant, "$mode surfaceVariant")
            assertNotEquals(default.surfaceTint, scheme.surfaceTint, "$mode surfaceTint")
            assertNotEquals(default.inversePrimary, scheme.inversePrimary, "$mode inversePrimary")
            assertNotEquals(default.primaryFixed, scheme.primaryFixed, "$mode primaryFixed")
            assertNotEquals(default.secondaryFixed, scheme.secondaryFixed, "$mode secondaryFixed")
            assertNotEquals(default.tertiaryFixed, scheme.tertiaryFixed, "$mode tertiaryFixed")
        }
    }

    @Test
    fun noRoleHasThePurpleHueOfMaterialsDefaultSchemes() {
        // The defaults' primary colors are purples; a role that is still one of them was missed.
        val purples = setOf(Color(0xFF6750A4), Color(0xFFD0BCFF), Color(0xFFEADDFF), Color(0xFF21005D), Color(0xFF625B71), Color(0xFFCCC2DC), Color(0xFF7D5260))
        for ((mode, scheme) in listOf("light" to light, "dark" to dark)) {
            val roles = listOf(
                scheme.primary, scheme.onPrimary, scheme.primaryContainer, scheme.onPrimaryContainer, scheme.secondary, scheme.secondaryContainer,
                scheme.tertiary, scheme.tertiaryContainer, scheme.inversePrimary, scheme.surfaceTint, scheme.primaryFixed, scheme.secondaryFixed,
            )
            assertTrue(roles.none { it in purples }, "$mode scheme still has a Material default purple")
        }
    }

    @Test
    fun theTwoSchemesDifferWhereTheyShouldAndShareTheGold() {
        assertNotEquals(light.background, dark.background)
        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light.onBackground, dark.onBackground)
        assertEquals(light.tertiary, dark.tertiary)
        assertEquals(light.onTertiary, dark.onTertiary)
    }

    @Test
    fun theFunctionGivesTheSameSchemeEveryTime() {
        assertEquals(drivingLogColorScheme(false), drivingLogColorScheme(false))
        assertEquals(drivingLogColorScheme(true), drivingLogColorScheme(true))
        assertNotEquals(drivingLogColorScheme(false), drivingLogColorScheme(true))
    }

    // ---- Accessibility

    @Test
    fun everyTextPairMeetsTheRuleInTheLightScheme() = assertAll(light, "light", textPairs)

    @Test
    fun everyTextPairMeetsTheRuleInTheDarkScheme() = assertAll(dark, "dark", textPairs)

    @Test
    fun everyOutlineIconAndControlMeetsTheRuleInTheLightScheme() = assertAll(light, "light", controlPairs)

    @Test
    fun everyOutlineIconAndControlMeetsTheRuleInTheDarkScheme() = assertAll(dark, "dark", controlPairs)

    @Test
    fun theSchemesAreOpaque() {
        for (scheme in listOf(light, dark)) {
            for (c in listOf(scheme.primary, scheme.background, scheme.surface, scheme.outline, scheme.error, scheme.onSurface)) {
                assertEquals(1f, c.alpha)
            }
        }
    }

    // ---- The distance and fuel colors

    @Test
    fun theDistanceColorIsTheBrightEmeraldInDarkAndADeeperShadeInLight() {
        assertEquals(RoadTripEmerald, domainColors(darkTheme = true).distance)
        assertEquals(EmeraldDeep, domainColors(darkTheme = false).distance)
        assertNotEquals(RoadTripEmerald, domainColors(darkTheme = false).distance)
    }

    @Test
    fun theLightDistanceColorIsReadableOnTheBackgroundAndOnACard() {
        val distance = domainColors(darkTheme = false).distance

        assertTrue(contrastRatio(distance, light.background) >= 4.5, "on the background")
        assertTrue(contrastRatio(distance, ExhaustFog) >= 4.5, "on Exhaust Fog")
        assertTrue(contrastRatio(distance, light.surfaceContainer) >= 4.5, "on a container")
    }

    @Test
    fun theDarkDistanceColorIsReadableOnTheBackgroundAndOnACard() {
        val distance = domainColors(darkTheme = true).distance

        assertTrue(contrastRatio(distance, dark.background) >= 4.5, "on the background")
        assertTrue(contrastRatio(distance, dark.surfaceVariant) >= 4.5, "on a variant surface")
        assertTrue(contrastRatio(distance, dark.surfaceContainer) >= 4.5, "on a container")
    }

    @Test
    fun theBrightEmeraldWouldNotHaveBeenReadableAsLightText() {
        assertTrue(contrastRatio(RoadTripEmerald, light.background) < 3.0)
    }

    @Test
    fun fuelIsTheSameGoldWithDarkTextInBothSchemes() {
        for (dark in listOf(false, true)) {
            val colors = domainColors(dark)
            assertEquals(FuelGaugeGold, colors.fuel)
            assertEquals(AsphaltDark, colors.onFuel)
            assertTrue(contrastRatio(colors.onFuel, colors.fuel) >= 4.5, "text on a gold fill, dark=$dark")
        }
    }

    @Test
    fun onlyTheDistanceColorDiffersBetweenTheSchemes() {
        val light = domainColors(false)
        val dark = domainColors(true)

        assertNotEquals(light.distance, dark.distance)
        assertEquals(light.fuel, dark.fuel)
        assertEquals(light.onFuel, dark.onFuel)
    }

    @Test
    fun goldIsNotUsableAsTextOnTheLightBackground() {
        // The reason the gold is only ever a fill with dark text, or a mark.
        assertTrue(contrastRatio(FuelGaugeGold, light.background) < 3.0)
    }

    // ---- The header

    @Test
    fun theHeaderIsPetroleumDeepWithLightContentInBothSchemes() {
        assertEquals(PetroleumDeep, HeaderContainer)
        assertEquals(CoolPlatinum, HeaderContent)
    }

    @Test
    fun theHeadersTitleAndIconsAreReadableOnIt() {
        // The header does not change with the scheme, so one ratio covers both, and it is far above the rule.
        assertTrue(contrastRatio(HeaderContent, HeaderContainer) >= 4.5)
        assertTrue(contrastRatio(HeaderContent, HeaderContainer) >= 3.0)
    }

    @Test
    fun aPrimaryTextButtonWouldNotHaveBeenReadableOnTheLightHeader() {
        // The reason the "Save" buttons in the header take the header's content color and not the default primary.
        assertTrue(contrastRatio(light.primary, HeaderContainer) < 3.0)
    }

    @Test
    fun theDarkHeaderIsHardlyToldApartFromTheDarkBackgroundSoADividerIsNeeded() {
        assertTrue(contrastRatio(HeaderContainer, dark.background) < 1.2)
        assertTrue(contrastRatio(dark.outlineVariant, dark.background) > 1.4)
    }
}
