package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
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
 * A kind of event the form can log. Only [DISTANCE] exists today; the Kind selector is disabled until a second one is added by a
 * later change, so [entries] having one member is what keeps it that way ([LogEventScreen]'s `KindSelector` reads it, not a
 * hand-written flag).
 */
enum class LogKind(val label: String) {
    DISTANCE("Distance"),
}

/** One vehicle as the selector draws it: its picture or icon, its name and, when it has one, its plate. In the order of [com.mikonoma.drivinglog.vehicle.domain.VehicleNameOrder]. */
data class VehicleChoice(
    val id: String,
    val name: String,
    val licensePlate: String?,
    val type: VehicleType,
    val color: Rgb,
    val pictureUri: String?,
)

/**
 * The log distance form. What the user typed and chose is saved so it survives rotation and process death; what comes from
 * the repository or is only a message ([Transient]) is rebuilt.
 */
@Serializable
data class LogEventState(
    /** The kind of event being logged. Only [LogKind.DISTANCE] exists today, and the selector that shows it is disabled; persisted (not [Transient]) so a later
     * kind's choice survives rotation and process death the way the vehicle choice does. */
    val kind: LogKind = LogKind.DISTANCE,
    val way: LogWay = LogWay.TRIP_DISTANCE,
    /** What is typed for the "Trip distance" way. Each way keeps its own number. */
    val tripDistance: OdometerEntry,
    /** What is typed for the "New odometer" way. */
    val newOdometer: OdometerEntry,
    /** The wall-clock date and time of the entry, in [zoneId]. Starts as the moment the form was opened. */
    val localDateTime: LocalDateTime,
    val zoneId: String,
    /**
     * The vehicle the entry is for. Empty until the form (opened from the Home screen, with a selector) has picked one: the
     * restored choice if that vehicle still exists, else the vehicle last logged for, else the first by name. From a vehicle's
     * details screen this is that vehicle's id from the moment the form opens, and the selector is not shown.
     */
    val selectedVehicleId: String = "",
    /** The id of the vehicle the unit was last set from, so switching to another vehicle (or, once, opening the form) sets it again but a restored state keeps what the user chose. */
    val unitFor: String = "",
    @Transient val isLoading: Boolean = true,
    @Transient val notFound: Boolean = false,
    @Transient val vehicleUnit: OdometerUnit = OdometerUnit.KILOMETERS,
    /** Every vehicle the selector offers, in order. Empty when the form has a fixed vehicle (opened from its details screen). */
    @Transient val vehicles: List<VehicleChoice> = emptyList(),
    /** The vehicle's whole log, newest first. */
    @Transient val log: List<VehicleEvent> = emptyList(),
    @Transient val error: LogDistanceError? = null,
    @Transient val isSaving: Boolean = false,
) : ViewState {

    /** The unit both fields are entered in. */
    val unit: OdometerUnit get() = tripDistance.unit

    val activeEntry: OdometerEntry get() = if (way == LogWay.TRIP_DISTANCE) tripDistance else newOdometer

    /** The selector shows this vehicle as chosen, or null before [vehicles] has loaded. */
    val selectedVehicle: VehicleChoice? get() = vehicles.firstOrNull { it.id == selectedVehicleId }

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

sealed interface LogEventIntent : ViewIntent {
    data class WayChanged(val way: LogWay) : LogEventIntent
    data class UnitFamilySelected(val miles: Boolean) : LogEventIntent
    data class TenthsChanged(val included: Boolean) : LogEventIntent

    /** The active field's new text from the system keyboard. */
    data class OdometerEdited(val text: String) : LogEventIntent
    data object OdometerCleared : LogEventIntent
    data class DateChanged(val date: LocalDate) : LogEventIntent
    data class TimeChanged(val hour: Int, val minute: Int) : LogEventIntent

    /** Keeps the wall-clock date and time and changes the zone they are in. */
    data class ZoneChanged(val zoneId: String) : LogEventIntent

    /** Chooses another vehicle in the selector (only offered when the form was opened without one). */
    data class VehicleSelected(val vehicleId: String) : LogEventIntent

    /** Chooses another kind of event in the Kind selector. The selector is disabled while [LogKind] has one entry, so nothing dispatches this yet. */
    data class KindSelected(val kind: LogKind) : LogEventIntent
    data object Save : LogEventIntent
}

sealed interface LogEventEffect : SideEffect {
    /** The entry was saved; go back to whatever opened the form (a vehicle's details, or the Home screen). */
    data object Saved : LogEventEffect
}
