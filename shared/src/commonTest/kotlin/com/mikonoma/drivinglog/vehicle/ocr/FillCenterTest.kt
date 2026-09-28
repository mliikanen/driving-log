package com.mikonoma.drivinglog.vehicle.ocr

import kotlin.test.Test
import kotlin.test.assertEquals

class FillCenterTest {
    private val box = TextBox(100, 50, 200, 90)

    @Test
    fun theSameAspectIsOnlyScaled() {
        val m = FillCenter(1280, 720, 640f, 360f)

        assertEquals(0.5f, m.scale)
        assertEquals(ViewRect(50f, 25f, 100f, 45f), m.map(box))
    }

    @Test
    fun aFrameWiderThanTheViewIsCroppedAtTheSides() {
        // 16:9 frame in a 9:16 portrait view: scaled to the view's height, centered horizontally.
        val m = FillCenter(1280, 720, 360f, 640f)

        assertEquals(640f / 720, m.scale)
        val offsetX = (360f - 1280 * m.scale) / 2
        assertEquals(ViewRect(100 * m.scale + offsetX, 50 * m.scale, 200 * m.scale + offsetX, 90 * m.scale), m.map(box))
    }

    @Test
    fun aFrameTallerThanTheViewIsCroppedAtTopAndBottom() {
        // 3:4 frame in a 16:9 landscape view: scaled to the view's width, centered vertically.
        val m = FillCenter(600, 800, 1600f, 900f)

        assertEquals(1600f / 600, m.scale)
        val offsetY = (900f - 800 * m.scale) / 2
        assertEquals(ViewRect(100 * m.scale, 50 * m.scale + offsetY, 200 * m.scale, 90 * m.scale + offsetY), m.map(box))
    }
}
