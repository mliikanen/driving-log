package com.mikonoma.drivinglog.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.landing.LandingIcon

/**
 * The icons of the Home screen's tiles, drawn from SVG path data the way [VehicleIcons] and [PhotoIcons] are (a single filled path on a 256 x 256 view box,
 * tinted by the content color of the tile). They are Phosphor glyphs, MIT licensed (see `THIRD_PARTY_NOTICES.md`); the original files are in `docs/icons/phosphor`.
 * The vehicles tile uses the generic car of [VehicleIcons].
 */
object LandingIcons {

    /** The vector a tile draws for [icon]. */
    fun of(icon: LandingIcon): ImageVector = when (icon) {
        LandingIcon.VEHICLES -> VehicleIcons.Car
        LandingIcon.ADD_VEHICLE -> AddVehicle
        LandingIcon.LOG_EVENT -> LogEvent
        LandingIcon.TRIP -> Trip
        LandingIcon.PLACEHOLDER -> Placeholder
    }

    /** `plus-circle-fill`: "Add vehicle". */
    val AddVehicle: ImageVector by lazy { build("AddVehicle", ADD_VEHICLE_PATH) }

    /** `note-pencil-fill`: "Log event". */
    val LogEvent: ImageVector by lazy { build("LogEvent", LOG_EVENT_PATH) }

    /** `path-fill`: "Trip". */
    val Trip: ImageVector by lazy { build("Trip", TRIP_PATH) }

    /** `question-fill`: the placeholder tile. */
    val Placeholder: ImageVector by lazy { build("Placeholder", PLACEHOLDER_PATH) }

    /** The path data of the new icons by name, for tests. */
    internal val pathData: Map<String, String> get() = mapOf(
        "AddVehicle" to ADD_VEHICLE_PATH,
        "LogEvent" to LOG_EVENT_PATH,
        "Trip" to TRIP_PATH,
        "Placeholder" to PLACEHOLDER_PATH,
    )

    private fun build(name: String, path: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 256f, viewportHeight = 256f)
            .addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
            .build()

    internal const val ADD_VEHICLE_PATH =
        "M128,24A104,104,0,1,0,232,128,104.13,104.13,0,0,0,128,24Zm40,112H136v32a8,8,0,0,1-16,0V136H88a8,8,0,0,1,0-16h32V88a8,8,0,0,1,16,0v32h32a8,8,0,0,1,0,16Z"
    internal const val LOG_EVENT_PATH =
        "M224,128v80a16,16,0,0,1-16,16H48a16,16,0,0,1-16-16V48A16,16,0,0,1,48,32h80a8,8,0,0,1,0,16H48V208H208V128a8,8,0,0,1,16,0Zm5.66-58.34-96,96A8,8,0,0,1,128,168H96a8,8,0,0,1-8-8V128a8,8,0,0,1,2.34-5.66l96-96a8,8,0,0,1,11.32,0l32,32A8,8,0,0,1,229.66,69.66Zm-17-5.66L192,43.31,179.31,56,200,76.69Z"
    internal const val TRIP_PATH =
        "M228,200a28,28,0,0,1-54.83,8H72a48,48,0,0,1,0-96h96a24,24,0,0,0,0-48H72a8,8,0,0,1,0-16h96a40,40,0,0,1,0,80H72a32,32,0,0,0,0,64H173.17A28,28,0,0,1,228,200Z"
    internal const val PLACEHOLDER_PATH =
        "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm0,168a12,12,0,1,1,12-12A12,12,0,0,1,128,192Zm8-48.72V144a8,8,0,0,1-16,0v-8a8,8,0,0,1,8-8c13.23,0,24-9,24-20s-10.77-20-24-20-24,9-24,20v4a8,8,0,0,1-16,0v-4c0-19.85,17.94-36,40-36s40,16.15,40,36C168,125.38,154.24,139.93,136,143.28Z"
}
