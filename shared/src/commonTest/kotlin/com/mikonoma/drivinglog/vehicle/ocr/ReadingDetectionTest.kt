package com.mikonoma.drivinglog.vehicle.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shapes the real test photos gave (`maestro/assets/ocr/`, design.md "Measured with ML Kit"), transcribed with their boxes:
 * what the recognizer returns, not the photos themselves.
 */
class ReadingDetectionTest {

    /** A line of words, each `text@left,top,right,bottom`; the line's box encloses them. */
    private fun line(vararg words: String): RecognizedLine {
        val elements = words.map { word ->
            val (text, box) = word.split('@')
            val (l, t, r, b) = box.split(',').map { it.toInt() }
            RecognizedElement(text, TextBox(l, t, r, b))
        }
        val box = TextBox(elements.minOf { it.box.left }, elements.minOf { it.box.top }, elements.maxOf { it.box.right }, elements.maxOf { it.box.bottom })
        return RecognizedLine(elements.joinToString(" ") { it.text }, box, elements)
    }

    private fun photo(vararg lines: RecognizedLine) = RecognizedPhoto(1280, 720, lines.toList())

    private fun List<Detection>.single(value: String) = single { it.value == value }

    /** The car cluster of `odo/20250831_073738`, as read at twice the size. */
    private val carCluster = photo(
        line("30@355,220,371,239"),
        line("917@445,246,481,264", "km@480,246,501,264"),
        line("Trip@391,275,419,293", "Average@422,275,483,293"),
        line("0.0vodkm@412,295,510,326"),
        line("ODO@524,403,554,415"),
        line("71140km@529,412,619,433"),
        line("7:36@645,118,716,149"),
        line("95@676,539,703,565", "E10@710,532,754,560"),
    )

    @Test
    fun anOdoLabelAboveDecides() {
        val odo = detectReadings(carCluster, knownOdometer = 71000.0).single("71140")

        assertEquals(ReadingKind.ODOMETER, odo.kind)
        assertEquals(DetectionBasis.LABEL, odo.basis)
        assertEquals("ODO", odo.label)
    }

    @Test
    fun aLabelDecidesRegardlessOfMagnitude() {
        val odo = detectReadings(carCluster, knownOdometer = 500.0).single("71140")

        assertEquals(ReadingKind.ODOMETER, odo.kind)
    }

    @Test
    fun zeroInALabelIsReadAsO() {
        val photo = photo(line("OD0@458,405,492,420"), line("71140@489,416,527,436"))

        assertEquals(ReadingKind.ODOMETER, detectReadings(photo, 71000.0).single("71140").kind)
    }

    @Test
    fun aTotalDistanceLabelBelowDecides() {
        val photo = photo(line("16865@412,434,452,448", "em@456,434,468,448"), line("Total@405,450,428,466", "distance@434,448,472,465"))

        val reading = detectReadings(photo, knownOdometer = null).single("16865")

        assertEquals(ReadingKind.ODOMETER, reading.kind)
        assertEquals("Total distance", reading.label)
    }

    @Test
    fun aTripLabelBeforeItOnItsLineDecides() {
        val photo = photo(line("TRIP@400,700,450,720", "209.1@460,700,540,725", "Km@545,705,570,725"))

        val reading = detectReadings(photo, knownOdometer = 3000.0).single("209.1")

        assertEquals(ReadingKind.TRIP, reading.kind)
        assertEquals(209, reading.whole)
        assertEquals(1, reading.tenth)
    }

    @Test
    fun aLoneTBeforeItOnItsLineDecides() {
        val photo = photo(line("T@440,690,460,720", "142.0@470,690,560,720"))

        assertEquals(ReadingKind.TRIP, detectReadings(photo, knownOdometer = 50000.0).single("142.0").kind)
    }

    /** `odo/20220911_162029` at twice the size: the clock 16:21 reads as `1521`, and the trip-mode "T" sits under it. */
    @Test
    fun aLoneTOnAnotherLineIsNotALabel() {
        val photo = photo(line("1521@500,533,590,560"), line("T@492,570,510,595"))

        val clock = detectReadings(photo, knownOdometer = 5000.0).single("1521")

        assertNull(clock.kind)
        assertEquals(DetectionBasis.NO_UNIT, clock.basis)
    }

    @Test
    fun aLabelTooFarAwayDoesNotCount() {
        val photo = photo(line("ODO@524,100,554,115"), line("71140km@529,412,619,433"))

        val reading = detectReadings(photo, knownOdometer = null).single("71140")

        assertEquals(DetectionBasis.MAGNITUDE, reading.basis)
    }

    @Test
    fun anUnlabeledValueNearTheKnownOdometerIsAnOdometerReading() {
        val photo = photo(line("32478@521,607,560,622", "km@562,607,577,622"))

        val reading = detectReadings(photo, knownOdometer = 32400.0).single("32478")

        assertEquals(ReadingKind.ODOMETER, reading.kind)
        assertEquals(DetectionBasis.MAGNITUDE, reading.basis)
    }

    @Test
    fun anUnlabeledSmallValueWithAUnitIsATrip() {
        val photo = photo(line("168.1@540,690,620,720", "Km@625,700,650,720"))

        assertEquals(ReadingKind.TRIP, detectReadings(photo, knownOdometer = 5000.0).single("168.1").kind)
    }

    /** "Trip Average" labels the consumption under the range, not the range: only a whole-line label counts. */
    @Test
    fun trippAverageDoesNotLabelTheRangeAsATrip() {
        val range = detectReadings(carCluster, knownOdometer = 71000.0).single("917")

        assertEquals(DetectionBasis.MAGNITUDE, range.basis)
    }

    @Test
    fun unitlessDialNumbersAreNotPresented() {
        val photo = photo(line("120@830,467,850,485"), line("RPMx@340,652,361,665", "1000@363,654,381,667"), line("240@895,598,927,616"))

        val detections = detectReadings(photo, knownOdometer = 5000.0)

        assertEquals(listOf("120", "1000", "240"), detections.map { it.value })
        assertTrue(detections.all { it.kind == null && it.basis == DetectionBasis.NO_UNIT })
        assertTrue(detections.candidates().isEmpty())
    }

    @Test
    fun aSpeedUnitIsNotADistanceUnit() {
        val photo = photo(line("130@600,500,640,530", "km/h@645,510,680,530"))

        assertNull(detectReadings(photo, knownOdometer = 5000.0).single("130").kind)
    }

    @Test
    fun clocksAndLetterPrefixedRangesAreNotNumbers() {
        val photo = photo(
            line("7:36@645,118,716,149"),
            line("19:37@558,457,596,469"),
            line("STANDNDD@565,595,634,617", "P890@678,606,707,623", "km@704,608,722,624"),
            line("D870@670,600,708,618", "km@710,601,728,619"),
            line("3-40@376,412,430,440"),
        )

        assertTrue(detectReadings(photo, knownOdometer = 50000.0).isEmpty())
    }

    @Test
    fun fewerThanThreeDigitsIsNotANumber() {
        val photo = photo(line("95@676,539,703,565", "km@710,539,730,565"), line("0.0vodkm@412,295,510,326"), line("000ufin@387,489,433,497"))

        assertTrue(detectReadings(photo, knownOdometer = 50000.0).isEmpty())
    }

    @Test
    fun moreDigitsThanAnOdometerHoldsIsNotANumber() {
        val photo = photo(line("ODO@524,403,554,415"), line("12345678km@529,412,619,433"))

        assertTrue(detectReadings(photo, knownOdometer = null).isEmpty())
    }

    @Test
    fun anImplausibleUnlabeledValueIsNotPresented() {
        val photo = photo(line("5200@521,607,560,622", "km@562,607,577,622"))

        val reading = detectReadings(photo, knownOdometer = 50000.0).single("5200")

        assertNull(reading.kind)
        assertEquals(DetectionBasis.IMPLAUSIBLE, reading.basis)
    }

    @Test
    fun aValueBelowTheKnownOdometerIsNotAnOdometerReading() {
        assertNull(kindByMagnitude(49999.0, 50000.0))
        assertEquals(ReadingKind.ODOMETER, kindByMagnitude(50000.0, 50000.0))
        assertEquals(ReadingKind.ODOMETER, kindByMagnitude(75000.0, 50000.0))
        assertNull(kindByMagnitude(75001.0, 50000.0))
        assertEquals(ReadingKind.ODOMETER, kindByMagnitude(8000.0, 3000.0))
    }

    @Test
    fun withoutAKnownOdometerMagnitudeAloneDecides() {
        assertEquals(ReadingKind.TRIP, kindByMagnitude(2000.0, null))
        assertEquals(ReadingKind.ODOMETER, kindByMagnitude(2000.1, null))
    }

    @Test
    fun aUnitRunOntoTheNumberCounts() {
        val photo = photo(line("917ki@385,267,436,283"))

        assertEquals(ReadingKind.TRIP, detectReadings(photo, knownOdometer = 71000.0).single("917").kind)
    }

    @Test
    fun distanceUnits() {
        for (unit in listOf("km", "Km", "knm", "ki", "mi", "miles")) assertTrue(isDistanceUnit(unit), unit)
        for (unit in listOf("", "km/h", "Knh", "Kh", "em", "E10", "m")) assertTrue(!isDistanceUnit(unit), unit)
    }

    @Test
    fun recognitionScaleDoublesSmallPhotosOnly() {
        assertEquals(2.0, recognitionScale(1280, 720))
        assertEquals(2560.0 / 1310, recognitionScale(983, 1310))
        assertEquals(1.0, recognitionScale(3072, 2304))
        assertEquals(2.0, recognitionScale(400, 300))
    }
}
