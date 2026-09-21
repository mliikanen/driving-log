package com.mikonoma.drivinglog.ui.color

import com.materialkolor.hct.Hct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorLibrarySmokeTest {

    @Test
    fun hctReadsTheToneOfARedAndRoundTripsIt() {
        val hct = Hct.fromInt(0xFFE53935.toInt())

        assertTrue(hct.tone in 40.0..60.0, "tone ${hct.tone}")
        assertEquals(0xFFE53935.toInt(), Hct.from(hct.hue, hct.chroma, hct.tone).toInt(), "round trip")
    }
}
