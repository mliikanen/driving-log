package com.mikonoma.drivinglog.vehicle.ocr

import kotlin.math.max

/** A rectangle on the screen, in pixels of the view that shows the camera's preview. */
data class ViewRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/**
 * Where a camera frame's pixels land in a view that shows it the way CameraX's `PreviewView` does with `FILL_CENTER` (`add-live-scanner`):
 * scaled by the larger of the two view/frame ratios, so the frame covers the whole view, and centered, the overflow cropped.
 */
class FillCenter(frameWidth: Int, frameHeight: Int, viewWidth: Float, viewHeight: Float) {
    val scale: Float = max(viewWidth / frameWidth, viewHeight / frameHeight)
    private val offsetX = (viewWidth - frameWidth * scale) / 2
    private val offsetY = (viewHeight - frameHeight * scale) / 2

    fun map(box: TextBox): ViewRect = ViewRect(box.left * scale + offsetX, box.top * scale + offsetY, box.right * scale + offsetX, box.bottom * scale + offsetY)
}
