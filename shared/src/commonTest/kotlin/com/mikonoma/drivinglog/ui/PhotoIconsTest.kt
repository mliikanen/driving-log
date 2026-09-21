package com.mikonoma.drivinglog.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoIconsTest {

    @Test
    fun theCameraHasPathDataOnA256ViewBox() {
        val camera = PhotoIcons.Camera

        assertEquals(256f, camera.viewportWidth)
        assertEquals(256f, camera.viewportHeight)
        assertTrue(PhotoIcons.CAMERA_PATH.length > 50)
        // Building parses the path data; an invalid one would have thrown by now.
        assertTrue(camera.root.size > 0)
    }

    @Test
    fun theCameraPathIsTheOneOfTheOriginalSvgFile() {
        assertTrue(PhotoIcons.CAMERA_PATH.startsWith("M208,56H180.28L166.65,35.56"))
    }
}
