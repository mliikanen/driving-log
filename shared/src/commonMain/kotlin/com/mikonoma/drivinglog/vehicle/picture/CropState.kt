package com.mikonoma.drivinglog.vehicle.picture

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The part of an image a crop keeps: a square of [side] pixels whose top-left corner is ([x], [y]), in the image's pixels. */
data class CropRect(val x: Int, val y: Int, val side: Int)

/**
 * The square the user frames on a photo. Everything is in the photo's own pixels, so the maths does not depend on the screen:
 * the screen turns a drag or a pinch into pixels of the photo (using the frame's on-screen size) and calls [panBy] or [zoomBy].
 *
 * The frame is a square of `min(width, height) / zoom` pixels, so zoom 1 is the largest square the photo holds and the frame
 * always fits. The centre is kept where the whole frame is inside the photo, and the zoom between 1 and [maxZoom], which keeps
 * the frame at least [MIN_SIDE] pixels (or the whole shorter side of a smaller photo). A state never leaves those limits.
 */
class CropState private constructor(
    val imageWidth: Int,
    val imageHeight: Int,
    val zoom: Float,
    val centerX: Float,
    val centerY: Float,
) {
    private val shorterSide = min(imageWidth, imageHeight)

    /** The frame's side, in pixels of the photo. */
    val side: Float get() = shorterSide / zoom

    /** The largest zoom: the frame is not made smaller than [MIN_SIDE] pixels, or than the photo itself when that is smaller. */
    val maxZoom: Float get() = max(1f, shorterSide.toFloat() / min(MIN_SIDE, shorterSide))

    /** Moves the frame over the photo by ([dx], [dy]) pixels of the photo, stopping at the photo's edges. */
    fun panBy(dx: Float, dy: Float): CropState = of(imageWidth, imageHeight, zoom, centerX + dx, centerY + dy)

    /**
     * Zooms by [factor] (above 1 closer, below 1 further out), keeping the point of the photo at ([focusX], [focusY]) at the
     * same place in the frame. The zoom stays within its limits and the frame inside the photo.
     */
    fun zoomBy(factor: Float, focusX: Float = centerX, focusY: Float = centerY): CropState {
        val newZoom = (zoom * factor).coerceIn(1f, maxZoom)
        val newSide = shorterSide / newZoom
        val relativeX = (focusX - (centerX - side / 2)) / side
        val relativeY = (focusY - (centerY - side / 2)) / side
        val left = focusX - relativeX * newSide
        val top = focusY - relativeY * newSide
        return of(imageWidth, imageHeight, newZoom, left + newSide / 2, top + newSide / 2)
    }

    /** The whole pixels the frame covers. Always inside the photo. */
    fun rect(): CropRect {
        val whole = side.roundToInt().coerceIn(1, shorterSide)
        val x = (centerX - side / 2).roundToInt().coerceIn(0, imageWidth - whole)
        val y = (centerY - side / 2).roundToInt().coerceIn(0, imageHeight - whole)
        return CropRect(x, y, whole)
    }

    override fun equals(other: Any?): Boolean =
        other is CropState && imageWidth == other.imageWidth && imageHeight == other.imageHeight &&
            zoom == other.zoom && centerX == other.centerX && centerY == other.centerY

    override fun hashCode(): Int = listOf(imageWidth, imageHeight, zoom, centerX, centerY).hashCode()

    override fun toString(): String = "CropState(${imageWidth}x$imageHeight, zoom=$zoom, centre=($centerX, $centerY))"

    companion object {
        /** The frame is never smaller than this many pixels of the photo (unless the photo is smaller). */
        const val MIN_SIDE = 128

        /** The start: the largest square, in the middle of the photo. */
        fun initial(imageWidth: Int, imageHeight: Int): CropState {
            require(imageWidth > 0 && imageHeight > 0) { "An image has a size" }
            return CropState(imageWidth, imageHeight, 1f, imageWidth / 2f, imageHeight / 2f)
        }

        private fun of(width: Int, height: Int, zoom: Float, centerX: Float, centerY: Float): CropState {
            val half = min(width, height) / zoom / 2
            return CropState(
                width, height, zoom,
                centerX.coerceIn(half, width - half),
                centerY.coerceIn(half, height - half),
            )
        }
    }
}
