package com.mikonoma.drivinglog.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventIconsTest {

    @Test
    fun theNoteIconHasPathDataOnA256ViewBoxThatBuilds() {
        val vector = EventIcons.Note

        assertEquals(256f, vector.viewportWidth)
        assertEquals(256f, vector.viewportHeight)
        // Building parses the path data; an invalid one would have thrown by now.
        assertTrue(vector.root.size > 0)
    }

    @Test
    fun theNotePathIsThatOfTheOriginalSvgFileAndNotThePencilOne() {
        // The file in docs/icons/phosphor is Phosphor's `note-fill`; a path that drifted from it would start differently.
        assertTrue(EventIcons.NOTE_PATH.startsWith("M208,32H48A16,16"))
        assertTrue(EventIcons.NOTE_PATH != LandingIcons.LOG_EVENT_PATH)
    }
}
