package com.mikonoma.drivinglog.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeTest {

    @Test
    fun darkThemeUsesTheDarkScheme() {
        val scheme = drivingLogColorScheme(darkTheme = true)

        assertEquals(AsphaltDark, scheme.background)
        // A dark scheme is dark: its background is darker than its text.
        assertTrue(relativeLuminance(scheme.background) < relativeLuminance(scheme.onBackground))
    }

    @Test
    fun lightThemeUsesTheLightScheme() {
        val scheme = drivingLogColorScheme(darkTheme = false)

        assertEquals(CoolPlatinum, scheme.background)
        assertTrue(relativeLuminance(scheme.background) > relativeLuminance(scheme.onBackground))
    }
}
