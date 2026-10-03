package com.mikonoma.drivinglog.vehicle.ocr.ppocr

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** A quadrilateral's corners; a perspective transform has eight unknowns (two per corner), the ninth element being 1. */
private const val CORNERS = 4
private const val UNKNOWNS = 8

/** Fewer points than this have no area: they are their own hull. */
private const val MIN_POLYGON_POINTS = 3
private const val MIDPOINT = 0.5

/** Lengths and pivots below these are treated as zero. */
private const val EPSILON = 1e-12
private const val DETERMINANT_EPSILON = 1e-18

/** A point in pixels. */
data class Point(val x: Double, val y: Double) {
    operator fun minus(o: Point) = Point(x - o.x, y - o.y)
    operator fun plus(o: Point) = Point(x + o.x, y + o.y)
    operator fun times(k: Double) = Point(x * k, y * k)
    fun length(): Double = hypot(x, y)
}

/** Four corners, in the order top-left, top-right, bottom-right, bottom-left, of a text box that may be rotated. */
data class Quad(val tl: Point, val tr: Point, val br: Point, val bl: Point) {
    val points: List<Point> get() = listOf(tl, tr, br, bl)
    val minX: Double get() = points.minOf { it.x }
    val maxX: Double get() = points.maxOf { it.x }
    val minY: Double get() = points.minOf { it.y }
    val maxY: Double get() = points.maxOf { it.y }

    /** True when ([x], [y]) is inside (or on the edge of) this convex quadrilateral. */
    fun contains(x: Double, y: Double): Boolean {
        var sign = 0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            val cross = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x)
            val s = if (cross > 1e-9) {
                1
            } else if (cross < -1e-9) {
                -1
            } else {
                0
            }
            if (s != 0) {
                if (sign == 0) {
                    sign = s
                } else if (s != sign) {
                    return false
                }
            }
        }
        return true
    }
}

/** A rectangle that may be rotated: its [center], its [width] and [height] along its own axes [u] and [v] (unit vectors). */
data class RotatedRect(val center: Point, val width: Double, val height: Double, val u: Point, val v: Point) {
    val shortSide: Double get() = min(width, height)

    /** The same rectangle with every side moved out by [distance]. */
    fun grown(distance: Double) = copy(width = width + 2 * distance, height = height + 2 * distance)

    /** Its corners, ordered like PaddleOCR's `get_mini_boxes`: sorted by x, the upper of the two left ones first, then the upper right one. */
    fun corners(): Quad {
        val hu = u * (width / 2)
        val hv = v * (height / 2)
        val pts = listOf(center - hu - hv, center + hu - hv, center + hu + hv, center - hu + hv).sortedBy { it.x }
        val (left, right) = pts.take(2) to pts.takeLast(2)
        val (topLeft, bottomLeft) = if (left[1].y > left[0].y) left[0] to left[1] else left[1] to left[0]
        val (topRight, bottomRight) = if (right[1].y > right[0].y) right[0] to right[1] else right[1] to right[0]
        return Quad(topLeft, topRight, bottomRight, bottomLeft)
    }
}

/** The convex hull of [points], counterclockwise (Andrew's monotone chain); collinear points are left out. */
fun convexHull(points: List<Point>): List<Point> {
    val sorted = points.distinct().sortedWith(compareBy({ it.x }, { it.y }))
    if (sorted.size < MIN_POLYGON_POINTS) return sorted
    fun cross(o: Point, a: Point, b: Point) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    val lower = mutableListOf<Point>()
    for (p in sorted) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), p) <= 0) lower.removeAt(lower.size - 1)
        lower += p
    }
    val upper = mutableListOf<Point>()
    for (p in sorted.asReversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), p) <= 0) upper.removeAt(upper.size - 1)
        upper += p
    }
    return lower.dropLast(1) + upper.dropLast(1)
}

/** The rectangle of smallest area around [points] (OpenCV's `minAreaRect`): one of its sides lies along an edge of their hull. */
fun minAreaRect(points: List<Point>): RotatedRect {
    val hull = convexHull(points)
    if (hull.size == 1) return RotatedRect(hull[0], 0.0, 0.0, Point(1.0, 0.0), Point(0.0, 1.0))
    if (hull.size == 2) {
        val d = hull[1] - hull[0]
        val len = d.length()
        val u = d * (1 / len)
        return RotatedRect((hull[0] + hull[1]) * MIDPOINT, len, 0.0, u, Point(-u.y, u.x))
    }
    var best: RotatedRect? = null
    var bestArea = Double.MAX_VALUE
    for (i in hull.indices) {
        val e = hull[(i + 1) % hull.size] - hull[i]
        val len = e.length()
        if (len < EPSILON) continue
        val u = e * (1 / len)
        val v = Point(-u.y, u.x)
        var minU = Double.MAX_VALUE
        var maxU = -Double.MAX_VALUE
        var minV = Double.MAX_VALUE
        var maxV = -Double.MAX_VALUE
        for (p in hull) {
            val pu = p.x * u.x + p.y * u.y
            val pv = p.x * v.x + p.y * v.y
            minU = min(minU, pu)
            maxU = max(maxU, pu)
            minV = min(minV, pv)
            maxV = max(maxV, pv)
        }
        val area = (maxU - minU) * (maxV - minV)
        if (area < bestArea) {
            bestArea = area
            val cu = (minU + maxU) / 2
            val cv = (minV + maxV) / 2
            best = RotatedRect(Point(u.x * cu + v.x * cv, u.y * cu + v.y * cv), maxU - minU, maxV - minV, u, v)
        }
    }
    return best!!
}

/**
 * The 3x3 perspective transform (row-major, last element 1) that takes [from]'s four corners to [to]'s, like OpenCV's
 * `getPerspectiveTransform`, solved as the usual 8x8 linear system.
 */
fun perspectiveTransform(from: List<Point>, to: List<Point>): DoubleArray {
    val a = Array(UNKNOWNS) { DoubleArray(UNKNOWNS + 1) }
    for (i in 0 until CORNERS) {
        val (x, y) = from[i]
        val (X, Y) = to[i]
        a[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -x * X, -y * X, X)
        a[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -x * Y, -y * Y, Y)
    }
    for (col in 0 until UNKNOWNS) {
        val pivot = (col until UNKNOWNS).maxBy { abs(a[it][col]) }
        val tmp = a[col]
        a[col] = a[pivot]
        a[pivot] = tmp
        val p = a[col][col]
        require(abs(p) > EPSILON) { "The corners do not make a quadrilateral" }
        for (c in col..UNKNOWNS) a[col][c] /= p
        for (r in 0 until UNKNOWNS) {
            if (r != col) {
                val f = a[r][col]
                if (f != 0.0) for (c in col..UNKNOWNS) a[r][c] -= f * a[col][c]
            }
        }
    }
    return DoubleArray(UNKNOWNS + 1) { if (it < UNKNOWNS) a[it][UNKNOWNS] else 1.0 }
}

/** [p] taken through the perspective transform [m] (from [perspectiveTransform]). */
@Suppress("MagicNumber") // m[0] to m[8] are the elements of a row-major 3x3 matrix, written as the formula reads.
fun applyTransform(m: DoubleArray, p: Point): Point {
    val w = m[6] * p.x + m[7] * p.y + m[8]
    return Point((m[0] * p.x + m[1] * p.y + m[2]) / w, (m[3] * p.x + m[4] * p.y + m[5]) / w)
}

/** The inverse of a 3x3 matrix, row-major. */
fun invert3(m: DoubleArray): DoubleArray {
    val a = m[0]
    val b = m[1]
    val c = m[2]
    val d = m[3]
    val e = m[4]
    val f = m[5]
    val g = m[6]
    val h = m[7]
    val i = m[8]
    val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
    require(abs(det) > DETERMINANT_EPSILON) { "Not invertible" }
    return doubleArrayOf(
        (e * i - f * h) / det, (c * h - b * i) / det, (b * f - c * e) / det,
        (f * g - d * i) / det, (a * i - c * g) / det, (c * d - a * f) / det,
        (d * h - e * g) / det, (b * g - a * h) / det, (a * e - b * d) / det,
    )
}

/** The Euclidean distance between two points. */
fun distance(a: Point, b: Point): Double = sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))
