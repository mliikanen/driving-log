package com.mikonoma.drivinglog.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icons about photos, drawn from SVG path data the way [VehicleIcons] are (a single filled path on a 256 x 256 view box, tinted by the
 * theme). The camera is the Phosphor `camera-fill` glyph, MIT licensed: see `THIRD_PARTY_NOTICES.md`; the original file is in
 * `docs/icons/phosphor`.
 */
object PhotoIcons {

    /** The mark on a picture preview that says a photo can be set by tapping it. */
    val Camera: ImageVector by lazy {
        ImageVector.Builder(
            name = "Camera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f,
        ).addPath(
            pathData = PathParser().parsePathString(CAMERA_PATH).toNodes(),
            fill = SolidColor(Color.Black),
        ).build()
    }

    /** The SVG path data of [Camera], for tests. */
    internal const val CAMERA_PATH =
        "M208,56H180.28L166.65,35.56A8,8,0,0,0,160,32H96a8,8,0,0,0-6.65,3.56L75.71,56H48A24,24,0,0,0,24,80V192a24,24,0,0,0,24,24H208a24,24,0,0,0,24-24V80A24,24,0,0,0,208,56Zm-44,76a36,36,0,1,1-36-36A36,36,0,0,1,164,132Z"
}
