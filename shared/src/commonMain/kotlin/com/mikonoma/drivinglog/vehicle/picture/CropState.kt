package com.mikonoma.drivinglog.vehicle.picture

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Where the frame is drawn on the screen: a square of [side] pixels whose top-left corner is ([left], [top]). */
class CropFrame(val left: Float, val top: Float, val side: Float)

/**
 * The frame for a screen whose free space (what the controls leave) is the rectangle from ([spaceLeft], [spaceTop]) to ([spaceRight], [spaceBottom]): the
 * largest square that keeps [margin] pixels clear of the space's edges, in the middle of it (never smaller than a pixel, so a tiny space cannot break the maths).
 */
fun cropFrame(spaceLeft: Float, spaceTop: Float, spaceRight: Float, spaceBottom: Float, margin: Float): CropFrame {
    val side = max(1f, min(spaceRight - spaceLeft, spaceBottom - spaceTop) - 2 * margin)
    val centerX = (spaceLeft + spaceRight) / 2
    val centerY = (spaceTop + spaceBottom) / 2
    return CropFrame(centerX - side / 2, centerY - side / 2, side)
}

/** The four ways the move buttons and the arrow keys move the photo under the frame. */
enum class CropMove { Left, Right, Up, Down }

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
// An immutable state with one function per crop operation (pan, zoom, move, rotate, save, restore).
@Suppress("TooManyFunctions")
class CropState private constructor(val imageWidth: Int, val imageHeight: Int, val zoom: Float, val centerX: Float, val centerY: Float) {
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

    /**
     * What a drag and a pinch do: the gesture moved the fingers' centre to ([centroidX], [centroidY]) on the screen, by ([panX], [panY]) pixels of the screen since
     * the last event, and scaled by [zoom] (above 1 is fingers apart). The photo follows the fingers: it moves by the pan, in the photo's pixels the pan divided by
     * the scale of [frame] (screen pixels per pixel of the photo), and the point of the photo under the fingers stays under them while zooming. This is the state the
     * gesture is applied to, so a change by a button is never lost; the limits of [panBy] and [zoomBy] hold.
     */
    fun transformedBy(frame: CropFrame, centroidX: Float, centroidY: Float, panX: Float, panY: Float, zoom: Float): CropState {
        val scale = frame.side / side // screen pixels per pixel of the photo
        val focusX = centerX - side / 2 + (centroidX - frame.left) / scale
        val focusY = centerY - side / 2 + (centroidY - frame.top) / scale
        return panBy(-panX / scale, -panY / scale).zoomBy(zoom, focusX, focusY)
    }

    /** Zooms in by one step, around the centre of the frame (the button "Zoom in"). Stops at [maxZoom]. */
    fun zoomInStep(): CropState = zoomBy(ZOOM_STEP)

    /** Zooms out by one step, around the centre of the frame (the button "Zoom out"). Stops at the smallest zoom. */
    fun zoomOutStep(): CropState = zoomBy(1f / ZOOM_STEP)

    /** Moves the photo under the frame by one step ([MOVE_STEP] of the frame's side) in [direction], stopping at the photo's edges. */
    fun movePhoto(direction: CropMove): CropState {
        val step = side * MOVE_STEP
        // The photo moves one way, so the frame moves the other way over it.
        return when (direction) {
            CropMove.Left -> panBy(step, 0f)
            CropMove.Right -> panBy(-step, 0f)
            CropMove.Up -> panBy(0f, step)
            CropMove.Down -> panBy(0f, -step)
        }
    }

    /**
     * The state for the photo turned a quarter turn clockwise, with the frame over the same part of it: the photo is then
     * [imageHeight] wide and [imageWidth] high, the frame is the same size, and a point (x, y) of the photo is at (height - y, x).
     */
    fun rotatedClockwise(): CropState = of(imageHeight, imageWidth, zoom, imageHeight - centerY, centerX)

    /** What [restore] needs to rebuild this state: the size of the photo, the zoom and the centre. */
    fun toSaved(): List<Float> = listOf(imageWidth.toFloat(), imageHeight.toFloat(), zoom, centerX, centerY)

    /** The whole pixels the frame covers. Always inside the photo. */
    fun rect(): CropRect {
        val whole = side.roundToInt().coerceIn(1, shorterSide)
        val x = (centerX - side / 2).roundToInt().coerceIn(0, imageWidth - whole)
        val y = (centerY - side / 2).roundToInt().coerceIn(0, imageHeight - whole)
        return CropRect(x, y, whole)
    }

    override fun equals(other: Any?): Boolean = other is CropState && imageWidth == other.imageWidth && imageHeight == other.imageHeight &&
        zoom == other.zoom && centerX == other.centerX && centerY == other.centerY

    override fun hashCode(): Int = listOf(imageWidth, imageHeight, zoom, centerX, centerY).hashCode()

    override fun toString(): String = "CropState(${imageWidth}x$imageHeight, zoom=$zoom, centre=($centerX, $centerY))"

    companion object {
        /** The frame is never smaller than this many pixels of the photo (unless the photo is smaller). */
        const val MIN_SIDE = 128

        /** One tap of "Zoom in" makes the frame's side 1/1.25 of what it was (a fifth smaller); "Zoom out" is the inverse. */
        const val ZOOM_STEP = 1.25f

        /** One tap of a move button moves the photo by a tenth of the frame's side. */
        const val MOVE_STEP = 0.1f

        // Positions in [toSaved]'s list.
        private const val SAVED_WIDTH = 0
        private const val SAVED_HEIGHT = 1
        private const val SAVED_ZOOM = 2
        private const val SAVED_CENTER_X = 3
        private const val SAVED_CENTER_Y = 4
        private const val SAVED_SIZE = 5

        /**
         * The state [saved] (from [toSaved]) describes, or null when there is none or it is for a photo other than [imageWidth] by [imageHeight]
         * (a saved crop means nothing on another photo). The values are brought back inside their limits, so a state never leaves them.
         */
        fun restore(saved: List<Float>?, imageWidth: Int, imageHeight: Int): CropState? {
            if (saved == null || saved.size != SAVED_SIZE) return null
            if (saved[SAVED_WIDTH].toInt() != imageWidth || saved[SAVED_HEIGHT].toInt() != imageHeight) return null
            val start = initial(imageWidth, imageHeight)
            return of(imageWidth, imageHeight, saved[SAVED_ZOOM].coerceIn(1f, start.maxZoom), saved[SAVED_CENTER_X], saved[SAVED_CENTER_Y])
        }

        /** The start: the largest square, in the middle of the photo. */
        fun initial(imageWidth: Int, imageHeight: Int): CropState {
            require(imageWidth > 0 && imageHeight > 0) { "An image has a size" }
            return CropState(imageWidth, imageHeight, 1f, imageWidth / 2f, imageHeight / 2f)
        }

        private fun of(width: Int, height: Int, zoom: Float, centerX: Float, centerY: Float): CropState {
            val half = min(width, height) / zoom / 2
            return CropState(
                width,
                height,
                zoom,
                centerX.coerceIn(half, width - half),
                centerY.coerceIn(half, height - half),
            )
        }
    }
}
