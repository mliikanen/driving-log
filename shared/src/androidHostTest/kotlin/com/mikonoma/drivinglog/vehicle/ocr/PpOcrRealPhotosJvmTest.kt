package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.ocr.ppocr.OnnxPpOcrModels
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.PpOcr
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The PP-OCR recognizer (`add-seven-segment-ocr`) over the real photos in `maestro/assets/ocr/`, with the model files the app ships,
 * on ONNX Runtime's JVM library: the port of RapidOCR's pipeline must read what the evaluation measured (design.md).
 */
class PpOcrRealPhotosJvmTest {
    private val modelsDir = File(System.getProperty("ocrModelsDir"))
    private val photosDir = File(System.getProperty("ocrPhotosDir"))
    private val models = OnnxPpOcrModels(
        File(modelsDir, "PP-OCRv6_det_tiny.onnx").readBytes(),
        File(modelsDir, "en_PP-OCRv5_rec_mobile.onnx").readBytes(),
        OnnxPpOcrModels.characters(File(modelsDir, "en_PP-OCRv5_rec_mobile.characters.txt").readText()),
    )

    @AfterTest
    fun close() = models.close()

    /** The words read in the photo [name]. */
    private fun words(name: String): List<String> {
        val image = checkNotNull(ImageIO.read(File(photosDir, name))) { "$name is not an image" }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return PpOcr.recognize(RgbImage(image.width, image.height, pixels), models).lines.flatMap { line -> line.elements.map { it.text } }
    }

    private fun assertReads(name: String, vararg expected: String) {
        val words = words(name)
        for (word in expected) assertTrue(word in words, "$name: expected \"$word\" among $words")
    }

    // The seven-segment LCD readings ML Kit cannot read.

    @Test
    fun theLcdOdometerReadings() {
        assertReads("odo/20220911_162029.jpg", "ODO", "5034")
        assertReads("odo/20230530_170748.jpg", "ODO", "5368")
        assertReads("trip/20220706_112423.jpg", "3056")
    }

    @Test
    fun theLcdTripReadings() {
        assertReads("trip/20220911_162031.jpg", "TRIP", "168.1")
        assertReads("trip/20230624_212428.jpg", "209.1")
    }

    // The car clusters: what the evaluation read, with its spaces (a label like "Total distance" is two words).

    @Test
    fun theCarClusters() {
        assertReads("odo/20250831_073738.jpg", "917km", "Trip", "Average")
        assertReads("odo/20251109_193736.jpg", "32478", "km", "890")
        assertReads("odo/20251228_190558.jpg", "50961", "km")
        assertReads("odo/20260221_193742.jpg", "16865km", "Total", "distance")
    }
}
