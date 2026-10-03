package com.mikonoma.drivinglog.vehicle.ocr

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CombinedTextRecognizerTest {

    private fun line(text: String, box: TextBox) = RecognizedLine(text, box, listOf(RecognizedElement(text, box)))

    private val printed = FakeTextRecognizer(RecognizedPhoto(1280, 720, listOf(line("71140km", TextBox(529, 412, 619, 433)))))
    private val lcd = FakeTextRecognizer(RecognizedPhoto(1280, 720, listOf(line("5034", TextBox(490, 690, 590, 730)))))

    private object Failing : TextRecognizer {
        override val isAvailable = true
        override suspend fun recognize(bytes: ByteArray): RecognizedPhoto? = error("the model failed to load")
        override suspend fun recognize(frame: com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage): RecognizedPhoto? = error("the model failed to load")
    }

    @Test
    fun theLinesOfBothAreReturned() = runTest {
        val photo = CombinedTextRecognizer(printed, lcd).recognize(byteArrayOf(1))!!

        assertEquals(listOf("71140km", "5034"), photo.lines.map { it.text })
        assertEquals(1280 to 720, photo.width to photo.height)
    }

    @Test
    fun oneFailingLeavesTheOthers() = runTest {
        val photo = CombinedTextRecognizer(Failing, lcd).recognize(byteArrayOf(1))!!

        assertEquals(listOf("5034"), photo.lines.map { it.text })
    }

    @Test
    fun notAnImageForEitherIsNotAnImage() = runTest {
        assertNull(CombinedTextRecognizer(printed, lcd).recognize(byteArrayOf()))
    }

    @Test
    fun anotherSizeIsScaledToTheFirsts() = runTest {
        val half = FakeTextRecognizer(RecognizedPhoto(640, 360, listOf(line("5034", TextBox(245, 345, 295, 365)))))

        val photo = CombinedTextRecognizer(printed, half).recognize(byteArrayOf(1))!!

        assertEquals(TextBox(490, 690, 590, 730), photo.lines.last().box)
    }

    @Test
    fun availableWhenEitherIs() {
        assertTrue(CombinedTextRecognizer(UnavailableTextRecognizer, lcd).isAvailable)
        assertFalse(CombinedTextRecognizer(UnavailableTextRecognizer).isAvailable)
    }

    @Test
    fun aFrameGoesToBoth() = runTest {
        val frame = com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage(4, 2, IntArray(8))

        val photo = CombinedTextRecognizer(printed, lcd).recognize(frame)!!

        assertEquals(listOf("71140km", "5034"), photo.lines.map { it.text })
        assertEquals(listOf(frame), printed.frames)
        assertEquals(listOf(frame), lcd.frames)
    }
}
