package com.mikonoma.drivinglog.vehicle.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A new vehicle's odometer from a scan (`scan-initial-odometer`), with the shapes the real test photos gave. */
class InitialOdometerDetectionTest {

    /** A line of words, each `text@left,top,right,bottom`. */
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

    private fun kinds(vararg lines: RecognizedLine) = detectInitialOdometer(photo(*lines)).associate { it.value to it.kind }

    @Test
    fun anOdoLabeledReadingIsOffered() {
        val detections = detectInitialOdometer(photo(line("ODO@524,403,554,415"), line("71140km@529,412,619,433")))

        val odo = detections.single { it.value == "71140" }
        assertEquals(ReadingKind.ODOMETER, odo.kind)
        assertEquals(DetectionBasis.LABEL, odo.basis)
    }

    @Test
    fun aTripLabeledReadingIsNotOfferedWhateverItsSize() {
        val k = kinds(line("TRIP@400,700,450,720", "168.1@460,700,540,725"), line("TRIP@400,800,450,820", "45260@460,800,540,825"))

        assertNull(k["168.1"])
        assertNull(k["45260"])
    }

    @Test
    fun anUnlabeledReadingWithAUnitIsOfferedWhateverItsSize() {
        val k = kinds(line("120@521,607,545,622", "km@548,607,562,622"), line("32478@521,650,560,665", "km@562,650,577,665"))

        assertEquals(ReadingKind.ODOMETER, k["120"])
        assertEquals(ReadingKind.ODOMETER, k["32478"])
    }

    @Test
    fun anUnlabeledUnitlessReadingIsOfferedOnlyAbove2000() {
        // trip/20220706_112423 as PP-OCR reads it: the LCD odometer alone; and a speedometer dial's scale.
        val k = kinds(line("3056@499,716,575,744"), line("120@830,467,850,485"), line("240@895,598,927,616"))

        assertEquals(ReadingKind.ODOMETER, k["3056"])
        assertNull(k["120"])
        assertNull(k["240"])
    }

    @Test
    fun rpmScalesAndClocksAreNotOffered() {
        val detections = detectInitialOdometer(photo(line("RPMx@340,652,361,665", "1000@363,654,381,667"), line("1621@491,520,560,540")))

        assertTrue(detections.all { it.kind == null && it.basis == DetectionBasis.NO_UNIT })
    }

    @Test
    fun anUnlabeledSmallDecimalWithoutAUnitIsNotOffered() {
        // trip/20230624_212428: "209.1" is a trip meter's reading; a new vehicle's odometer is not read from it.
        assertNull(kinds(line("209.1@466,697,583,736"))["209.1"])
    }

    @Test
    fun theUnitIsRecordedWithTheDetection() {
        val detections = detectReadings(photo(line("917@445,246,481,264", "km@480,246,501,264"), line("3056@499,716,575,744")), knownOdometer = null)

        assertTrue(detections.single { it.value == "917" }.hasUnit)
        assertTrue(!detections.single { it.value == "3056" }.hasUnit)
    }
}
