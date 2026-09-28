package com.mikonoma.drivinglog.vehicle.ocr.ppocr

import com.mikonoma.drivinglog.vehicle.ocr.TextBox
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The pure steps of the PP-OCR pipeline (`add-seven-segment-ocr`), each against what RapidOCR computes for the same input. */
class PpOcrTest {

    private fun near(expected: Double, actual: Double, tolerance: Double = 1e-6) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, was $actual")

    // Sizes

    @Test
    fun sizesRoundToMultiplesOf32HalvesToEven() {
        assertEquals(32, PpOcr.roundTo32(16)) // 0.5 rounds to 0, and a side is at least 32
        assertEquals(64, PpOcr.roundTo32(48)) // 1.5 rounds to 2
        assertEquals(64, PpOcr.roundTo32(80)) // 2.5 rounds to 2
        assertEquals(736, PpOcr.roundTo32(736))
    }

    @Test
    fun detectionScalesTheShorterSideUpTo736() {
        assertEquals(1312 to 736, PpOcr.detectionSize(1280, 720))
        assertEquals(992 to 1312, PpOcr.detectionSize(983, 1310))
        assertEquals(1024 to 768, PpOcr.detectionSize(1024, 768)) // already large enough: only rounded
    }

    @Test
    fun aLargePhotoIsBroughtUnder2000FirstAndASmallOneIsLeft() {
        assertEquals(1984 to 1344, PpOcr.boundedSize(3000, 2000))
        assertEquals(1280 to 720, PpOcr.boundedSize(1280, 720))
    }

    // Tensors

    @Test
    fun aTensorIsBgrPlanesScaledToMinusOneToOne() {
        val image = RgbImage(2, 1, intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt())) // red, blue
        val t = PpOcr.tensor(image)
        assertEquals(listOf(-1f, 1f), t.slice(0..1)) // B plane
        assertEquals(listOf(-1f, -1f), t.slice(2..3)) // G plane
        assertEquals(listOf(1f, -1f), t.slice(4..5)) // R plane
    }

    @Test
    fun aRecognitionBatchIsPaddedWithZerosOnTheRight() {
        val line = RgbImage(96, 48, IntArray(96 * 48) { 0xFFFFFFFF.toInt() }) // white: 1.0 in every channel
        val width = PpOcr.batchWidth(listOf(line))
        assertEquals(320, width) // at least the recognizer's 320 x 48
        val t = PpOcr.recognitionBatch(listOf(line), width)
        assertEquals(3 * 48 * 320, t.size)
        assertEquals(1f, t[95]) // the line, scaled to 96 wide at 48 high
        assertEquals(0f, t[96]) // the padding after it
    }

    @Test
    fun aWideLineWidensItsBatch() {
        val wide = RgbImage(480, 48, IntArray(480 * 48))
        assertEquals(480, PpOcr.batchWidth(listOf(wide, RgbImage(48, 48, IntArray(48 * 48)))))
    }

    // Resizing

    @Test
    fun resizingKeepsAFlatColorAndInterpolatesAnEdge() {
        val grey = RgbImage(4, 4, IntArray(16) { 0xFF808080.toInt() })
        assertTrue(grey.resized(7, 3).pixels.all { it == 0xFF808080.toInt() })
        val edge = RgbImage(2, 1, intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())).resized(4, 1)
        assertEquals(listOf(0, 64, 191, 255), edge.pixels.map { it and 0xFF }) // OpenCV's half-pixel-aligned bilinear
    }

    // Detection map to boxes

    /** A map of [w] x [h] zeros with the probability [p] over the axis-aligned rectangle [x0]..[x1) x [y0]..[y1). */
    private fun map(w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, p: Float = 0.9f) =
        FloatArray(w * h) { i -> if (i % w in x0 until x1 && i / w in y0 until y1) p else 0f }

    @Test
    fun aRegionBecomesItsRectangleGrownByTheUnclipDistance() {
        // A 40 x 10 region at (20, 30); the 2x2 dilation adds a column on the right and a row below, so its pixels' centers
        // span x 20..60 and y 30..40: a 40 x 10 rectangle.
        val quads = PpOcr.boxes(map(100, 80, 20, 30, 60, 40), 100, 80, 100, 80)
        assertEquals(1, quads.size)
        val q = quads.single()
        // Grown by area * 1.6 / perimeter = 400 * 1.6 / 100 = 6.4 on each side, then rounded to whole pixels.
        near(20 - 6.4, q.minX, 1.0)
        near(60 + 6.4, q.maxX, 1.0)
        near(30 - 6.4, q.minY, 1.0)
        near(40 + 6.4, q.maxY, 1.0)
    }

    @Test
    fun boxesAreScaledToTheImage() {
        val q = PpOcr.boxes(map(100, 80, 20, 30, 60, 40), 100, 80, 200, 160).single()
        near(2 * (20 - 6.4), q.minX, 1.0)
        near(2 * (40 + 6.4), q.maxY, 1.0)
    }

    @Test
    fun aFaintRegionAndATinyOneAreDropped() {
        assertTrue(PpOcr.boxes(map(100, 80, 20, 30, 60, 40, p = 0.4f), 100, 80, 100, 80).isEmpty()) // above 0.3, but its mean is under 0.5
        assertTrue(PpOcr.boxes(map(100, 80, 20, 30, 22, 31), 100, 80, 100, 80).isEmpty()) // too thin to be text
    }

    @Test
    fun twoRegionsAreTwoBoxes() {
        val m = map(100, 80, 10, 10, 40, 20)
        val other = map(100, 80, 50, 50, 90, 60)
        for (i in m.indices) m[i] = maxOf(m[i], other[i])
        assertEquals(2, PpOcr.boxes(m, 100, 80, 100, 80).size)
    }

    // Geometry

    @Test
    fun theMinimumAreaRectangleOfARotatedRectangleIsIt() {
        val a = 0.3
        val u = Point(cos(a), sin(a))
        val v = Point(-sin(a), cos(a))
        val corners = listOf(Point(0.0, 0.0), u * 40.0, u * 40.0 + v * 10.0, v * 10.0)
        val rect = minAreaRect(corners + listOf(u * 20.0 + v * 5.0))
        near(400.0, rect.width * rect.height, 1e-6)
        near(10.0, rect.shortSide, 1e-6)
    }

    @Test
    fun cornersAreOrderedClockwiseFromTheTopLeft() {
        val q = PpOcr.clockwise(listOf(Point(10.0, 20.0), Point(0.0, 0.0), Point(10.0, 0.0), Point(0.0, 20.0)))
        assertEquals(Quad(Point(0.0, 0.0), Point(10.0, 0.0), Point(10.0, 20.0), Point(0.0, 20.0)), q)
    }

    @Test
    fun aPerspectiveTransformMapsCornersToCorners() {
        val from = listOf(Point(10.0, 5.0), Point(50.0, 12.0), Point(48.0, 30.0), Point(8.0, 22.0))
        val to = listOf(Point(0.0, 0.0), Point(40.0, 0.0), Point(40.0, 18.0), Point(0.0, 18.0))
        val m = perspectiveTransform(from, to)
        for (i in 0 until 4) {
            val p = applyTransform(m, from[i])
            near(to[i].x, p.x, 1e-9); near(to[i].y, p.y, 1e-9)
        }
        val back = applyTransform(invert3(m), to[2])
        near(48.0, back.x, 1e-9); near(30.0, back.y, 1e-9)
    }

    // Cropping

    /** An image whose pixel at (x, y) is red x, green y. */
    private fun gradient(w: Int, h: Int) = RgbImage(w, h, IntArray(w * h) { i -> (0xFF shl 24) or ((i % w) shl 16) or ((i / w) shl 8) })

    @Test
    fun anUprightBoxIsCroppedAsIs() {
        val crop = PpOcr.crop(gradient(100, 100), Quad(Point(10.0, 20.0), Point(50.0, 20.0), Point(50.0, 35.0), Point(10.0, 35.0)))
        assertEquals(40, crop.width)
        assertEquals(15, crop.height)
        assertEquals(10, crop.channel(0, 0, 0)) // red: x
        assertEquals(20, crop.channel(0, 0, 1)) // green: y
    }

    @Test
    fun aTallBoxIsTurnedUpright() {
        val crop = PpOcr.crop(gradient(100, 100), Quad(Point(10.0, 10.0), Point(20.0, 10.0), Point(20.0, 60.0), Point(10.0, 60.0)))
        assertEquals(50, crop.width)
        assertEquals(10, crop.height)
    }

    // Recognition decoding

    /** Scores where column t puts all its weight on class [best][t]. */
    private fun scores(vararg best: Int, classes: Int = 6) =
        RecognitionScores(FloatArray(best.size * classes) { i -> if (i % classes == best[i / classes]) 0.9f else 0.02f }, 1, best.size, classes)

    private val characters = listOf("1", "2", "3", "4") // classes 1..4; 0 is the blank, 5 the space

    @Test
    fun ctcDropsRepeatsAndBlanks() {
        val (text, score) = PpOcr.decode(scores(1, 1, 0, 2, 2), 0, characters)
        assertEquals("12", text)
        near(0.9, score, 1e-6) // the mean best score of the two kept columns
        assertEquals("11", PpOcr.decode(scores(1, 0, 1), 0, characters).first) // a blank between two of the same keeps both
    }

    @Test
    fun theLastClassIsASpace() {
        assertEquals("1 2", PpOcr.decode(scores(1, 5, 2), 0, characters).first)
    }

    @Test
    fun onlyBlanksReadNothingWithAZeroScore() {
        assertEquals("" to 0.0, PpOcr.decode(scores(0, 0, 0), 0, characters))
    }

    // Lines into words

    @Test
    fun wordsGetTheirShareOfTheLineByCharacterPosition() {
        val line = PpOcr.line("ODO 5034", Quad(Point(0.0, 10.0), Point(80.0, 10.0), Point(80.0, 30.0), Point(0.0, 30.0)))
        assertEquals(listOf("ODO", "5034"), line.elements.map { it.text })
        assertEquals(TextBox(0, 10, 30, 30), line.elements[0].box)
        assertEquals(TextBox(40, 10, 80, 30), line.elements[1].box)
    }

    // Frames (add-live-scanner)

    @Test
    fun aFrameIsTurnedUprightClockwise() {
        // 3 x 2: pixel values are their index. Turned a quarter clockwise it is 2 x 3, its first row the old first column, bottom up.
        val frame = RgbImage(3, 2, IntArray(6) { it })
        val upright = frame.turnedClockwise(1)
        assertEquals(2 to 3, upright.width to upright.height)
        assertEquals(listOf(3, 0, 4, 1, 5, 2), upright.pixels.toList())
        assertEquals(frame.pixels.toList(), frame.turnedClockwise(4).pixels.toList())
        assertEquals(frame.turnedClockwise(3).pixels.toList(), frame.turnedCounterclockwise().pixels.toList())
    }

    @Test
    fun aTextBoxInARotatedFrameComesBackUpright() {
        // A frame to be turned 90 degrees clockwise to be upright (a phone held upright, the camera's rotationDegrees 90): a white bar
        // across the top of the upright scene lies along the frame's left edge. Turned upright, it is at the top again.
        val w = 20; val h = 10
        val frame = RgbImage(w, h, IntArray(w * h) { i -> if (i % w < 2) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() })
        val upright = frame.turnedClockwise(1)
        assertEquals(10 to 20, upright.width to upright.height)
        assertTrue((0 until upright.width).all { x -> upright.pixels[x] == 0xFFFFFFFF.toInt() && upright.pixels[upright.width + x] == 0xFFFFFFFF.toInt() })
        assertTrue((0 until upright.width).all { x -> upright.pixels[2 * upright.width + x] == 0xFF000000.toInt() })
    }
}
