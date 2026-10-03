package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.data.FakeClock
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.OnnxPpOcrModels
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.PpOcr
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

/**
 * The live scanner's frame path (`add-live-scanner`) over the real photos in `maestro/assets/ocr/`, used as camera frames: PP-OCR
 * (on ONNX Runtime's JVM library; ML Kit needs a device), classification and the tracker, as each analyzed frame goes through them.
 */
class LiveFramesJvmTest {
    private val modelsDir = File(requireNotNull(System.getProperty("ocrModelsDir")) { "ocrModelsDir is set by the Gradle test task" })
    private val photosDir = File(requireNotNull(System.getProperty("ocrPhotosDir")) { "ocrPhotosDir is set by the Gradle test task" })
    private val models = OnnxPpOcrModels(
        File(modelsDir, "PP-OCRv6_det_tiny.onnx").readBytes(),
        File(modelsDir, "en_PP-OCRv5_rec_mobile.onnx").readBytes(),
        OnnxPpOcrModels.characters(File(modelsDir, "en_PP-OCRv5_rec_mobile.characters.txt").readText()),
    )
    private val clock = FakeClock()

    @AfterTest
    fun close() = models.close()

    private fun image(name: String): RgbImage {
        val image = checkNotNull(ImageIO.read(File(photosDir, name)))
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return RgbImage(image.width, image.height, pixels)
    }

    /** One analyzed frame: recognized, classified against [known], as the scanner does. */
    private fun analyze(frame: RgbImage, known: Double) = LiveFrame(frame, detectReadings(PpOcr.recognize(frame, models), known))

    private fun LiveReadings.values() = shown.associate { it.detection.value to it.detection.kind }

    @Test
    fun theLcdReadingsAreTrackedWithTheirKind() {
        for ((name, known, value, kind) in listOf(
            Quad("odo/20220911_162029.jpg", 5000.0, "5034", ReadingKind.ODOMETER),
            Quad("odo/20230530_170748.jpg", 5300.0, "5368", ReadingKind.ODOMETER),
            Quad("trip/20220706_112423.jpg", 3000.0, "3056", ReadingKind.ODOMETER),
            Quad("trip/20220911_162031.jpg", 5000.0, "168.1", ReadingKind.TRIP),
            Quad("trip/20230624_212428.jpg", 5000.0, "209.1", ReadingKind.TRIP),
        )) {
            val readings = LiveReadings(clock)
            readings.update(analyze(image(name), known))
            assertEquals(kind, readings.values()[value], "$name: ${readings.values()}")
        }
    }

    @Test
    fun aReadingSurvivesAFrameThatMissesIt() {
        val readings = LiveReadings(clock)
        val photo = image("odo/20220911_162029.jpg")
        val first = readings.update(analyze(photo, 5000.0)).single { it.detection.value == "5034" }

        clock.current += 1000.milliseconds
        readings.update(analyze(RgbImage(photo.width, photo.height, IntArray(photo.width * photo.height) { 0xFF000000.toInt() }), 5000.0))
        assertEquals(ReadingKind.ODOMETER, readings.values()["5034"], "still shown one frame later")

        clock.current += 1000.milliseconds
        val again = readings.update(analyze(photo, 5000.0)).single { it.detection.value == "5034" }
        assertEquals(first.id, again.id)
    }

    private data class Quad(val name: String, val known: Double, val value: String, val kind: ReadingKind)
}
