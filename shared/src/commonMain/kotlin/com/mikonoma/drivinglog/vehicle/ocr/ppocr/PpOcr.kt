package com.mikonoma.drivinglog.vehicle.ocr.ppocr

import com.mikonoma.drivinglog.vehicle.ocr.RecognizedElement
import com.mikonoma.drivinglog.vehicle.ocr.RecognizedLine
import com.mikonoma.drivinglog.vehicle.ocr.RecognizedPhoto
import com.mikonoma.drivinglog.vehicle.ocr.TextBox
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * The two PP-OCR models (`add-seven-segment-ocr`), which only a platform can run. Both take a batch of images as a float tensor
 * `[n, 3, height, width]`, channels in BGR order, as the models were trained with OpenCV's images.
 */
interface PpOcrModels {
    /** The detection model's probability map for one image: `height x width` values, row by row. */
    fun detect(input: FloatArray, width: Int, height: Int): FloatArray

    /** The recognition model's scores for a batch of [count] line images `48 x width`: `count x steps x classes` values. */
    fun recognize(input: FloatArray, count: Int, width: Int): RecognitionScores

    /** The recognizer's characters (its character list file); class 0 is the CTC blank, 1..n these, and n + 1 a space. */
    val characters: List<String>
}

/** What the recognizer scored: for each of [count] lines, [steps] columns of [classes] scores, row by row. */
class RecognitionScores(val values: FloatArray, val count: Int, val steps: Int, val classes: Int)

/**
 * RapidOCR's PP-OCR pipeline, ported with its constants (design.md, "The PP-OCR pipeline"), around the models the platform runs.
 * Everything here is arithmetic on pixels and scores, and is the same on every platform.
 *
 * Changed from RapidOCR v3.9.2's Python (Apache License 2.0): translated to Kotlin; see `THIRD_PARTY_NOTICES.md`.
 * Copyright (c) 2021 RapidOCR Authors. All rights reserved.
 * Copyright (c) 2020 PaddlePaddle Authors. All Rights Reserved.
 */
// Ported from RapidOCR's PP-OCR pipeline (design.md of add-seven-segment-ocr) and kept structurally parallel to it, so results can be compared line by line.
@Suppress("TooManyFunctions")
object PpOcr {
    const val MIN_SIDE = 30
    const val MAX_SIDE = 2000
    const val DETECTION_MIN_SIDE = 736
    const val DETECTION_THRESHOLD = 0.3f
    const val BOX_THRESHOLD = 0.5
    const val UNCLIP_RATIO = 1.6
    const val MIN_BOX_SIDE = 3
    const val REC_HEIGHT = 48
    const val REC_WIDTH = 320
    const val REC_BATCH = 6
    const val TEXT_SCORE = 0.5

    /** Detection sizes are multiples of the detection model's stride. */
    private const val DETECTION_STRIDE = 32

    /** A text box at least this much taller than wide is a vertical line, turned to read horizontally. */
    private const val VERTICAL_LINE_ASPECT = 1.5

    // Each model input value is `(v / 255 - 0.5) / 0.5`, per channel of a 0xAARRGGBB pixel, planes in BGR order.
    private const val MAX_CHANNEL = 255f
    private const val NORMALIZE_MEAN = 0.5f
    private const val NORMALIZE_STD = 0.5f
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BLUE_SHIFT = 0
    private const val CHANNEL_MASK = 0xFF

    private fun normalized(pixel: Int, shift: Int): Float = ((((pixel shr shift) and CHANNEL_MASK) / MAX_CHANNEL) - NORMALIZE_MEAN) / NORMALIZE_STD

    /** A size rounded to the nearest multiple of 32, halves to even as Python's `round` does, at least 32. */
    fun roundTo32(v: Int): Int = max(DETECTION_STRIDE, round(v / DETECTION_STRIDE.toDouble()).toInt() * DETECTION_STRIDE)

    /** The size the whole photo is brought to first: its longer side at most [MAX_SIDE] (then rounded to 32), its shorter side at least [MIN_SIDE]. */
    fun boundedSize(width: Int, height: Int): Pair<Int, Int> {
        var w = width
        var h = height
        if (max(w, h) > MAX_SIDE) {
            val ratio = MAX_SIDE.toDouble() / max(w, h)
            w = roundTo32((w * ratio).toInt())
            h = roundTo32((h * ratio).toInt())
        }
        if (min(w, h) < MIN_SIDE) {
            val ratio = MIN_SIDE.toDouble() / min(w, h)
            w = roundTo32((w * ratio).toInt())
            h = roundTo32((h * ratio).toInt())
        }
        return w to h
    }

    /** The detection model's input size for a [width] x [height] image: its shorter side at least [DETECTION_MIN_SIDE], each side a multiple of 32. */
    fun detectionSize(width: Int, height: Int): Pair<Int, Int> {
        val ratio = if (min(width, height) < DETECTION_MIN_SIDE) DETECTION_MIN_SIDE.toDouble() / min(width, height) else 1.0
        return roundTo32((width * ratio).toInt()) to roundTo32((height * ratio).toInt())
    }

    /** [image] as a `[1, 3, h, w]` tensor, BGR, each value `(v / 255 - 0.5) / 0.5`. */
    fun tensor(image: RgbImage): FloatArray {
        val plane = image.width * image.height
        val out = FloatArray(3 * plane)
        for (i in 0 until plane) {
            val p = image.pixels[i]
            out[i] = normalized(p, BLUE_SHIFT)
            out[plane + i] = normalized(p, GREEN_SHIFT)
            out[2 * plane + i] = normalized(p, RED_SHIFT)
        }
        return out
    }

    /**
     * The text boxes in a detection [map] of `mapWidth x mapHeight` probabilities, in the pixels of a `destWidth x destHeight` image
     * (DB post-processing): pixels above [DETECTION_THRESHOLD], dilated 2x2, form regions; each region's minimum-area rectangle is kept
     * when the map's mean inside it is at least [BOX_THRESHOLD], grown by the unclip distance, scaled and clipped to the image.
     */
    // Ported from RapidOCR's PP-OCR pipeline (design.md of add-seven-segment-ocr) and kept structurally parallel to it, so results can be compared line by line. The same for this function's branches and loop exits.
    @Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
    fun boxes(map: FloatArray, mapWidth: Int, mapHeight: Int, destWidth: Int, destHeight: Int): List<Quad> {
        val mask = BooleanArray(map.size) { map[it] > DETECTION_THRESHOLD }
        val dilated = BooleanArray(map.size)
        // OpenCV's 2x2 dilation, anchored at the kernel's center (1, 1): a pixel is set when it or its left, upper or upper-left neighbour is.
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                dilated[y * mapWidth + x] = mask[y * mapWidth + x] ||
                    (x > 0 && mask[y * mapWidth + x - 1]) ||
                    (y > 0 && mask[(y - 1) * mapWidth + x]) ||
                    (x > 0 && y > 0 && mask[(y - 1) * mapWidth + x - 1])
            }
        }
        val result = mutableListOf<Quad>()
        for (region in regions(dilated, mapWidth, mapHeight)) {
            val rect = minAreaRect(boundary(region, dilated, mapWidth, mapHeight))
            if (rect.shortSide < MIN_BOX_SIDE) continue
            val corners = rect.corners()
            if (BOX_THRESHOLD > meanInside(map, mapWidth, mapHeight, corners)) continue
            val area = rect.width * rect.height
            val perimeter = 2 * (rect.width + rect.height)
            val grown = rect.grown(area * UNCLIP_RATIO / perimeter)
            if (grown.shortSide < MIN_BOX_SIDE + 2) continue
            val scaled = grown.corners().points.map { p ->
                Point(
                    // NumPy's round: halves to even.
                    round(p.x / mapWidth * destWidth).coerceIn(0.0, destWidth.toDouble()),
                    round(p.y / mapHeight * destHeight).coerceIn(0.0, destHeight.toDouble()),
                )
            }
            val quad = clockwise(scaled).let { q ->
                Quad(clip(q.tl, destWidth, destHeight), clip(q.tr, destWidth, destHeight), clip(q.br, destWidth, destHeight), clip(q.bl, destWidth, destHeight))
            }
            if (distance(quad.tl, quad.tr).toInt() <= MIN_BOX_SIDE || distance(quad.tl, quad.bl).toInt() <= MIN_BOX_SIDE) continue
            result += quad
        }
        return result
    }

    /** The 8-connected regions of set pixels, each as its pixel indices. */
    // Ported from RapidOCR's PP-OCR pipeline (design.md of add-seven-segment-ocr) and kept structurally parallel to it, so results can be compared line by line. The same for this flood fill's nesting and bounds check.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun regions(mask: BooleanArray, w: Int, h: Int): List<IntArray> {
        val seen = BooleanArray(mask.size)
        val out = mutableListOf<IntArray>()
        val stack = IntArray(mask.size)
        for (start in mask.indices) {
            if (!mask[start] || seen[start]) continue
            var top = 0
            stack[top++] = start
            seen[start] = true
            val pixels = mutableListOf<Int>()
            while (top > 0) {
                val i = stack[--top]
                pixels += i
                val x = i % w
                val y = i / w
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                        val j = ny * w + nx
                        if (mask[j] && !seen[j]) {
                            seen[j] = true
                            stack[top++] = j
                        }
                    }
                }
            }
            out += pixels.toIntArray()
        }
        return out
    }

    /** The region's boundary pixels (those with a 4-neighbour outside it), as points: the pixels OpenCV's contour holds. */
    private fun boundary(region: IntArray, mask: BooleanArray, w: Int, h: Int): List<Point> = region.filter { i ->
        val x = i % w
        val y = i / w
        x == 0 || y == 0 || x == w - 1 || y == h - 1 || !mask[i - 1] || !mask[i + 1] || !mask[i - w] || !mask[i + w]
    }.map { Point((it % w).toDouble(), (it / w).toDouble()) }

    /** The map's mean over the pixels inside [quad] (RapidOCR's "fast" box score). */
    private fun meanInside(map: FloatArray, w: Int, h: Int, quad: Quad): Double {
        val x0 = kotlin.math.floor(quad.minX).toInt().coerceIn(0, w - 1)
        val x1 = ceil(quad.maxX).toInt().coerceIn(0, w - 1)
        val y0 = kotlin.math.floor(quad.minY).toInt().coerceIn(0, h - 1)
        val y1 = ceil(quad.maxY).toInt().coerceIn(0, h - 1)
        var sum = 0.0
        var n = 0
        for (y in y0..y1) {
            for (x in x0..x1) {
                if (quad.contains(x.toDouble(), y.toDouble())) {
                    sum += map[y * w + x]
                    n++
                }
            }
        }
        return if (n == 0) 0.0 else sum / n
    }

    /** Four points ordered top-left, top-right, bottom-right, bottom-left (by x, then by y within each side). */
    fun clockwise(points: List<Point>): Quad {
        val byX = points.sortedBy { it.x }
        val (tl, bl) = byX.take(2).sortedBy { it.y }
        val (tr, br) = byX.drop(2).sortedBy { it.y }
        return Quad(tl, tr, br, bl)
    }

    private fun clip(p: Point, w: Int, h: Int) =
        Point(p.x.coerceIn(0.0, (w - 1).toDouble()).toInt().toDouble(), p.y.coerceIn(0.0, (h - 1).toDouble()).toInt().toDouble())

    /**
     * The text inside [quad], taken out of [image] and straightened (a perspective warp onto an upright rectangle, edges replicated),
     * turned a quarter counterclockwise when it is at least one and a half times taller than wide.
     */
    fun crop(image: RgbImage, quad: Quad): RgbImage {
        val w = max(distance(quad.tl, quad.tr), distance(quad.br, quad.bl)).toInt().coerceAtLeast(1)
        val h = max(distance(quad.tl, quad.bl), distance(quad.tr, quad.br)).toInt().coerceAtLeast(1)
        val toSource =
            invert3(
                perspectiveTransform(
                    quad.points,
                    listOf(Point(0.0, 0.0), Point(w.toDouble(), 0.0), Point(w.toDouble(), h.toDouble()), Point(0.0, h.toDouble())),
                ),
            )
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val s = applyTransform(toSource, Point(x.toDouble(), y.toDouble()))
                out[y * w + x] = RgbImage.rgb(image.sample(s.x, s.y, 0), image.sample(s.x, s.y, 1), image.sample(s.x, s.y, 2))
            }
        }
        val cropped = RgbImage(w, h, out)
        return if (h.toDouble() / w >= VERTICAL_LINE_ASPECT) cropped.turnedCounterclockwise() else cropped
    }

    /** One batch of line images as the recognizer's input: each 48 px high, [batchWidth] wide, its own width by its aspect (at most the batch width), zero-padded on the right. */
    fun recognitionBatch(lines: List<RgbImage>, batchWidth: Int): FloatArray {
        val plane = REC_HEIGHT * batchWidth
        val out = FloatArray(lines.size * 3 * plane)
        lines.forEachIndexed { n, line ->
            val w = min(ceil(REC_HEIGHT * line.width.toDouble() / line.height).toInt(), batchWidth).coerceAtLeast(1)
            val resized = line.resized(w, REC_HEIGHT)
            val base = n * 3 * plane
            for (y in 0 until REC_HEIGHT) {
                for (x in 0 until w) {
                    val p = resized.pixels[y * w + x]
                    val i = y * batchWidth + x
                    out[base + i] = normalized(p, BLUE_SHIFT)
                    out[base + plane + i] = normalized(p, GREEN_SHIFT)
                    out[base + 2 * plane + i] = normalized(p, RED_SHIFT)
                }
            }
        }
        return out
    }

    /** The width of a batch: 48 px times the widest aspect ratio among [lines], and at least the recognizer's 320 x 48. */
    fun batchWidth(lines: List<RgbImage>): Int {
        val ratio = max(REC_WIDTH.toDouble() / REC_HEIGHT, lines.maxOf { it.width.toDouble() / it.height })
        return (REC_HEIGHT * ratio).toInt()
    }

    /** CTC greedy decoding of one line's scores: the best class per column, repeats and the blank (class 0) dropped; the score is the mean best score of the kept columns. */
    fun decode(scores: RecognitionScores, line: Int, characters: List<String>): Pair<String, Double> {
        val text = StringBuilder()
        var sum = 0.0
        var kept = 0
        var previous = -1
        for (t in 0 until scores.steps) {
            val base = (line * scores.steps + t) * scores.classes
            var best = 0
            var bestScore = scores.values[base]
            for (c in 1 until scores.classes) {
                val v = scores.values[base + c]
                if (v > bestScore) {
                    best = c
                    bestScore = v
                }
            }
            if (best != previous && best != 0) {
                text.append(characterOf(best, characters))
                sum += bestScore
                kept++
            }
            previous = best
        }
        return text.toString() to if (kept == 0) 0.0 else sum / kept
    }

    /** Class [index]'s character: 0 is the blank, 1..n the model's characters, n + 1 a space. */
    private fun characterOf(index: Int, characters: List<String>): String = when {
        index in 1..characters.size -> characters[index - 1]
        index == characters.size + 1 -> " "
        else -> ""
    }

    /** A recognized line as words: its text split on spaces, each word given the share of the line's box its characters span. */
    fun line(text: String, quad: Quad): RecognizedLine {
        val box = TextBox(quad.minX.toInt(), quad.minY.toInt(), ceil(quad.maxX).toInt(), ceil(quad.maxY).toInt())
        val elements = mutableListOf<RecognizedElement>()
        var start = 0
        for (word in text.split(' ')) {
            if (word.isNotEmpty()) {
                val left = box.left + box.width * start / text.length
                val right = box.left + box.width * (start + word.length) / text.length
                elements += RecognizedElement(word, TextBox(left, box.top, max(right, left + 1), box.bottom))
            }
            start += word.length + 1
        }
        return RecognizedLine(text.trim(), box, elements)
    }

    /** The whole pipeline over [photo] with [models]: detection, then recognition in batches sorted by aspect ratio; lines scoring under [TEXT_SCORE] are dropped. */
    fun recognize(photo: RgbImage, models: PpOcrModels): RecognizedPhoto {
        val (bw, bh) = boundedSize(photo.width, photo.height)
        val bounded = photo.resized(bw, bh)
        val (dw, dh) = detectionSize(bounded.width, bounded.height)
        val map = models.detect(tensor(bounded.resized(dw, dh)), dw, dh)
        val quads = boxes(map, dw, dh, bounded.width, bounded.height)
        val crops = quads.map { crop(bounded, it) }
        val order = crops.indices.sortedBy { crops[it].width.toDouble() / crops[it].height }
        val texts = arrayOfNulls<Pair<String, Double>>(crops.size)
        for (batch in order.chunked(REC_BATCH)) {
            val lines = batch.map { crops[it] }
            val width = batchWidth(lines)
            val scores = models.recognize(recognitionBatch(lines, width), lines.size, width)
            batch.forEachIndexed { n, index -> texts[index] = decode(scores, n, models.characters) }
        }
        // Back to the photo's own pixels.
        val sx = photo.width.toDouble() / bounded.width
        val sy = photo.height.toDouble() / bounded.height
        val result = quads.indices.mapNotNull { i ->
            val (text, score) = texts[i] ?: return@mapNotNull null
            if (score < TEXT_SCORE || text.isBlank()) return@mapNotNull null
            val q = quads[i]
            line(text, Quad(Point(q.tl.x * sx, q.tl.y * sy), Point(q.tr.x * sx, q.tr.y * sy), Point(q.br.x * sx, q.br.y * sy), Point(q.bl.x * sx, q.bl.y * sy)))
        }
        return RecognizedPhoto(photo.width, photo.height, result)
    }
}
