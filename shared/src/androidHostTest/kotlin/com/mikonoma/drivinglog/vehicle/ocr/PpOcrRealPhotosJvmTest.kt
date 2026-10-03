package com.mikonoma.drivinglog.vehicle.ocr

import com.mikonoma.drivinglog.vehicle.ocr.ppocr.OnnxPpOcrModels
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.PpOcr
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The PP-OCR recognizer (`add-seven-segment-ocr`) over the real photos in `maestro/assets/ocr/`, with the model files the app ships,
 * on ONNX Runtime's JVM library: the port of RapidOCR's pipeline must read what the evaluation measured (design.md).
 */
class PpOcrRealPhotosJvmTest {
    private val modelsDir = File(requireNotNull(System.getProperty("ocrModelsDir")) { "ocrModelsDir is set by the Gradle test task" })
    private val photosDir = File(requireNotNull(System.getProperty("ocrPhotosDir")) { "ocrPhotosDir is set by the Gradle test task" })
    private val models = OnnxPpOcrModels(
        File(modelsDir, "PP-OCRv6_det_tiny.onnx").readBytes(),
        File(modelsDir, "en_PP-OCRv5_rec_mobile.onnx").readBytes(),
        OnnxPpOcrModels.characters(File(modelsDir, "en_PP-OCRv5_rec_mobile.characters.txt").readText()),
    )

    @AfterTest
    fun close() = models.close()

    private fun recognize(name: String): RecognizedPhoto {
        val image = checkNotNull(ImageIO.read(File(photosDir, name))) { "$name is not an image" }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return PpOcr.recognize(RgbImage(image.width, image.height, pixels), models)
    }

    /** The words read in the photo [name]. */
    private fun words(name: String): List<String> = recognize(name).lines.flatMap { line -> line.elements.map { it.text } }

    /** The fuel-amount candidates [name] actually produces, as "value (label)", in no particular order. */
    private fun fuelAmountCandidates(name: String): List<String> = detectFuelAmount(recognize("fuel/$name")).candidates().map { "${it.value} (${it.label})" }

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

    // The fuel pump/receipt displays (add-fuel-amount-ocr): nine real photos, five languages, none in English —
    // read exactly as design.md's evaluation table records, including the OCR noise on two of them.

    @Test
    fun theFuelPumpDisplays() {
        assertReads("fuel/DSC_0025.jpg", "EUROA", "LITRAA") // Finnish, Dresser Wayne
        assertReads("fuel/DSC_0100.jpg", "dm^3") // Polish, PRONAR/ZAP
        assertReads("fuel/DSC_0105.jpg", "dm") // Polish, Dresser (the exponent is lost here)
        assertReads("fuel/DSC_0426.jpg", "IZNOS", "KUNA", "IZDANO", "LITARA") // Croatian
        assertReads("fuel/DSC_0477.jpg", "ZNESEK", "EUR", "CENA", "EUR/L") // Slovenian; "LITROV" misreads as "UITROV" here (see design.md)
        assertReads("fuel/DSC_0896.jpg", "EUROA", "LITRAA", "€/litra") // Finnish, truck diesel
        assertReads("fuel/IMAG0297.jpg", "EUROA", "LITRAA", "€/litra") // Finnish
        assertReads("fuel/IMAG0303.jpg", "€/L") // Finnish, Gilbarco — no volume label in frame at all
        assertReads("fuel/IMAG0305.jpg", "EUR") // Finnish, truck diesel; "LITRAA" misreads as "LITE" here (see design.md)
    }

    // What detectFuelAmount actually classifies from each photo (design.md, "The evaluation" and "More than one
    // candidate can share a label"): a real volume label close to more than one number on a dense pump display
    // picks up every one of them, not only the true reading — accepted, the same way several odometer/trip
    // candidates in one photo already are, and left for the review screen's existing "pick a candidate" step.

    @Test
    fun aDenseDisplayCanSurfaceMoreThanOneCandidate() {
        // DSC_0025.jpg: the amount (20.00) and an unrelated fine-print number (5) share LITRAA's adjacency with
        // the real reading (2.12, itself a digit misread of 12.12 — digit-level noise is a separate, accepted gap).
        assertEquals(setOf("20.00 (LITRAA)", "2.12 (LITRAA)", "5 (LITRAA)"), fuelAmountCandidates("DSC_0025.jpg").toSet())
        // DSC_0100.jpg: the money amount (5954) and the price-per-liter rate (5.20) likewise share dm^3's adjacency
        // with the real reading (1145, i.e. 11.45).
        assertEquals(setOf("5954 (dm^3)", "1145 (dm^3)", "5.20 (dm^3)"), fuelAmountCandidates("DSC_0100.jpg").toSet())
    }

    @Test
    fun aSingleNearbyNumberIsTheOnlyCandidate() {
        assertEquals(listOf("7.82 (LITRAA)"), fuelAmountCandidates("DSC_0896.jpg"))
        assertEquals(listOf("18.73 (LITRAA)"), fuelAmountCandidates("IMAG0297.jpg"))
    }

    @Test
    fun aMisreadOrMissingLabelClassifiesNothing() {
        // DSC_0105.jpg, DSC_0426.jpg: the recognizer never finds the reading's own digits at all on these two.
        // DSC_0477.jpg, IMAG0305.jpg: the digits are found, but the adjacent label misreads as "UITROV"/"LITE",
        // neither in VOLUME_LABELS (an accepted gap, not chased — see design.md's evaluation).
        // IMAG0303.jpg: correctly has no volume label in frame at all.
        for (name in listOf("DSC_0105.jpg", "DSC_0426.jpg", "DSC_0477.jpg", "IMAG0305.jpg", "IMAG0303.jpg")) {
            assertEquals(emptyList(), fuelAmountCandidates(name), name)
        }
    }
}
