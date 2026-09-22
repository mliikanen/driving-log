package com.mikonoma.drivinglog.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icons about a vehicle's logged events, drawn from SVG path data the way [VehicleIcons] and [PhotoIcons] are (a single filled path
 * on a 256 x 256 view box, tinted by the theme). The note glyph is the Phosphor `note-fill` glyph (a plain sheet, deliberately not the
 * `note-pencil-fill` glyph [LandingIcons.LogEvent] uses, so a row that has a note does not look editable from here), MIT licensed: see
 * `THIRD_PARTY_NOTICES.md`; the original file is in `docs/icons/phosphor`.
 */
object EventIcons {

    /** The mark on a log row whose event has a note (`add-event-notes`). */
    val Note: ImageVector by lazy { build("Note", NOTE_PATH) }

    private fun build(name: String, path: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 256f, viewportHeight = 256f)
            .addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
            .build()

    /** The SVG path data of [Note], for tests. */
    internal const val NOTE_PATH =
        "M208,32H48A16,16,0,0,0,32,48V208a16,16,0,0,0,16,16H156.69A15.92,15.92,0,0,0,168,219.31L219.31,168A15.92,15.92,0,0,0,224,156.69V48A16,16,0,0,0,208,32ZM96,88h64a8,8,0,0,1,0,16H96a8,8,0,0,1,0-16Zm32,80H96a8,8,0,0,1,0-16h32a8,8,0,0,1,0,16ZM96,136a8,8,0,0,1,0-16h64a8,8,0,0,1,0,16Zm64,68.69V160h44.7Z"
}
