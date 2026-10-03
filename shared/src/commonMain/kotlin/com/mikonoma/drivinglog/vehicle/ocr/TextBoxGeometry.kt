package com.mikonoma.drivinglog.vehicle.ocr

import kotlin.math.abs
import kotlin.math.max

/** [sameRowNearby]'s sideways reach: one and a half box heights, counted in half heights. */
private const val MAX_GAP_HALF_HEIGHTS = 3

/** On the same row (vertically overlapping by half the lower box) and at most one and a half box heights apart sideways. */
internal fun sameRowNearby(a: TextBox, b: TextBox): Boolean {
    val rows = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
    val height = minOf(a.height, b.height)
    if (height <= 0 || rows < height / 2) return false
    val gap = maxOf(a.left, b.left) - minOf(a.right, b.right)
    return gap <= maxOf(a.height, b.height) * MAX_GAP_HALF_HEIGHTS / 2
}

internal fun samePlace(a: TextBox, b: TextBox): Boolean {
    val w = minOf(a.right, b.right) - maxOf(a.left, b.left)
    val h = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
    if (w <= 0 || h <= 0) return false
    val smaller = minOf(a.width.toLong() * a.height, b.width.toLong() * b.height).coerceAtLeast(1)
    return w.toLong() * h >= smaller * ReadingThresholds.SAME_PLACE
}

/** The vertical space between two boxes, zero when they overlap vertically. */
internal fun verticalGap(a: TextBox, b: TextBox): Int = max(0, max(a.top - b.bottom, b.top - a.bottom))

/** The boxes overlap horizontally, or are apart by at most [number]'s width. */
internal fun horizontallyNear(label: TextBox, number: TextBox): Boolean {
    val gap = max(label.left - number.right, number.left - label.right)
    return gap <= 0 || abs(gap) <= number.width
}
