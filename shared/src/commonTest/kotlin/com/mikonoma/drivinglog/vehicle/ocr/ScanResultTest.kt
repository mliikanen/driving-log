package com.mikonoma.drivinglog.vehicle.ocr

import kotlin.test.Test
import kotlin.test.assertEquals

class ScanResultTest {

    private val result = ScanResult(
        width = 1280,
        height = 720,
        detections = listOf(
            Detection("917", "917", TextBox(445, 246, 481, 264), ReadingKind.TRIP, DetectionBasis.MAGNITUDE),
            Detection("71140km", "71140", TextBox(529, 412, 619, 433), ReadingKind.ODOMETER, DetectionBasis.LABEL, "ODO"),
            Detection("1000", "1000", TextBox(363, 654, 381, 667), null, DetectionBasis.NO_UNIT),
        ),
        acceptedIndex = 1,
    )

    @Test
    fun everyDetectionRoundTrips() {
        val read = ScanResult.fromJson(result.toJson())

        assertEquals(result, read)
        assertEquals("71140", read.accepted.value)
    }

    /** A result stored by a later version, with a field this one does not know (a marked region, say), still reads. */
    @Test
    fun anUnknownFieldIsIgnored() {
        val later = result.toJson().replaceFirst("{", """{"markedRegion":{"left":1,"top":2,"right":3,"bottom":4},""")

        assertEquals(result, ScanResult.fromJson(later))
    }
}
