package com.mikonoma.drivinglog.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeTest {

    @Test
    fun darkThemeUsesDarkColorScheme() {
        assertEquals(darkColorScheme().background, drivingLogColorScheme(darkTheme = true).background)
    }

    @Test
    fun lightThemeUsesLightColorScheme() {
        assertEquals(lightColorScheme().background, drivingLogColorScheme(darkTheme = false).background)
    }
}
