package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.data.FakeClock
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class LiveReadingsTest {
    private val clock = FakeClock()
    private val readings = LiveReadings(clock)

    private fun odo(value: String, box: TextBox) = Detection(value, value, box, ReadingKind.ODOMETER, DetectionBasis.LABEL, "ODO")
    private fun frame(vararg detections: Detection) = LiveFrame(RgbImage(1, 1, IntArray(1)), detections.toList())

    private val here = TextBox(500, 400, 600, 430)

    @Test
    fun aNewReadingAppears() {
        val shown = readings.update(frame(odo("71140", here)))

        assertEquals(listOf("71140"), shown.map { it.detection.value })
    }

    @Test
    fun aReadingAtTheSamePlaceKeepsItsIdentityAndTakesTheNewBox() {
        val first = readings.update(frame(odo("71140", here))).single()
        clock.current += 900.milliseconds
        val moved = TextBox(510, 405, 610, 435)

        val second = readings.update(frame(odo("71140", moved))).single()

        assertEquals(first.id, second.id)
        assertEquals(moved, second.detection.box)
    }

    @Test
    fun aMisreadIsCorrectedByTheNextFrameAtTheSamePlace() {
        val first = readings.update(frame(odo("71146", here))).single()

        val second = readings.update(frame(odo("71140", here))).single()

        assertEquals(first.id, second.id)
        assertEquals("71140", second.detection.value)
    }

    @Test
    fun aReadingMissedByAFrameStaysForTheGracePeriodThenGoes() {
        readings.update(frame(odo("71140", here)))
        clock.current += 1000.milliseconds
        assertEquals(1, readings.update(frame()).size)
        clock.current += 400.milliseconds
        assertEquals(1, readings.update(frame()).size) // 1.4 s since last seen
        clock.current += 200.milliseconds
        assertTrue(readings.update(frame()).isEmpty()) // 1.6 s
    }

    @Test
    fun expiringWithoutANewFrameDropsOldReadings() {
        readings.update(frame(odo("71140", here)))
        clock.current += 2000.milliseconds

        assertTrue(readings.expire().isEmpty())
    }

    @Test
    fun readingsElsewhereAreSeparate() {
        val shown = readings.update(frame(odo("71140", here), odo("917", TextBox(100, 100, 150, 120))))

        assertEquals(2, shown.map { it.id }.distinct().size)
    }

    @Test
    fun eachReadingKeepsTheFrameItWasLastSeenIn() {
        val older = frame(odo("71140", here), Detection("917", "917", TextBox(100, 100, 150, 120), ReadingKind.TRIP, DetectionBasis.MAGNITUDE))
        readings.update(older)
        val newer = frame(odo("71140", here))

        val shown = readings.update(newer)

        assertSame(newer, shown.single { it.detection.value == "71140" }.frame)
        assertSame(older, shown.single { it.detection.value == "917" }.frame) // missed by the newer frame, still within the grace period
    }

    @Test
    fun readingsThatAreNotCandidatesAreNotShown() {
        val dial = Detection("120", "120", TextBox(800, 400, 820, 420), null, DetectionBasis.NO_UNIT)

        assertTrue(readings.update(frame(dial)).isEmpty())
    }
}
