package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.domain.knownOdometerAt
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.fuusio.kide.presentation.SideEffect
import org.fuusio.kide.presentation.ViewIntent
import org.fuusio.kide.presentation.ViewState

/**
 * The log distance form. What the user typed and chose is saved so it survives rotation and process death; what comes from
 * the repository or is only a message ([Transient]) is rebuilt.
 */
@Serializable
data class LogDistanceState(
    val way: LogWay = LogWay.TRIP_DISTANCE,
    /** What is typed for the "Trip distance" way. Each way keeps its own number. */
    val tripDistance: OdometerEntry,
    /** What is typed for the "New odometer" way. */
    val newOdometer: OdometerEntry,
    /** The wall-clock date and time of the entry, in [zoneId]. Starts as the moment the form was opened. */
    val localDateTime: LocalDateTime,
    val zoneId: String,
    /** True once the unit has been set from the vehicle's, so a restored state keeps what the user chose. */
    val unitInitialized: Boolean = false,
    @Transient val isLoading: Boolean = true,
    @Transient val notFound: Boolean = false,
    @Transient val vehicleUnit: OdometerUnit = OdometerUnit.KILOMETERS,
    /** The vehicle's whole log, newest first. */
    @Transient val log: List<VehicleEvent> = emptyList(),
    @Transient val error: LogDistanceError? = null,
    @Transient val isSaving: Boolean = false,
) : ViewState {

    /** The unit both fields are entered in. */
    val unit: OdometerUnit get() = tripDistance.unit

    val activeEntry: OdometerEntry get() = if (way == LogWay.TRIP_DISTANCE) tripDistance else newOdometer

    /** The instant and zone the wall-clock time and zone id mean. */
    val moment: ZonedMoment
        get() = runCatching { momentOf(localDateTime, zoneId) }.getOrElse { momentOf(localDateTime, "UTC") }

    /** The previous known odometer at the chosen moment, or null when none is known at that time. */
    val knownOdometer: Distance? get() = knownOdometerAt(log.asReversed(), moment.instant)

    /** For "New odometer": the distance the typed count means, once something is typed and it is higher than the known one. */
    val previewDistance: Distance?
        get() {
            val known = knownOdometer ?: return null
            val typed = newOdometer.toDistance() ?: return null
            return distanceByOdometer(typed, known)
        }
}

sealed interface LogDistanceIntent : ViewIntent {
    data class WayChanged(val way: LogWay) : LogDistanceIntent
    data class UnitFamilySelected(val miles: Boolean) : LogDistanceIntent
    data class TenthsChanged(val included: Boolean) : LogDistanceIntent

    /** The active field's new text from the system keyboard. */
    data class OdometerEdited(val text: String) : LogDistanceIntent
    data object OdometerCleared : LogDistanceIntent
    data class DateChanged(val date: LocalDate) : LogDistanceIntent
    data class TimeChanged(val hour: Int, val minute: Int) : LogDistanceIntent

    /** Keeps the wall-clock date and time and changes the zone they are in. */
    data class ZoneChanged(val zoneId: String) : LogDistanceIntent
    data object Save : LogDistanceIntent
}

sealed interface LogDistanceEffect : SideEffect {
    /** The entry was saved; go back to the details. */
    data object Saved : LogDistanceEffect
}
