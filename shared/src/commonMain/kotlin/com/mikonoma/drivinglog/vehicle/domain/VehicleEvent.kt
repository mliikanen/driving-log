package com.mikonoma.drivinglog.vehicle.domain

/**
 * One fact in a vehicle's append-only log (`atomic-log-events`): an odometer reading, a distance or a refueling. Every
 * event belongs to exactly one entry ([entryId]): everything one action saved (adding the vehicle, or saving the log event
 * form), which holds the note and photos shared by all of its events.
 */
sealed interface VehicleEvent {
    val id: String

    /** When the event happened, in the time zone it was entered in. Every event of one entry has the same moment. */
    val occurredAt: ZonedMoment

    /** The entry the event belongs to. An entry migrated from before entries existed has its only event's id. */
    val entryId: String

    /** Where the event's entry was saved from. */
    val source: EntrySource

    /** The event's kind, which orders the events of one entry ([Kind.rank]). */
    val kind: Kind

    /**
     * The odometer reading this event sets, or null for an event that does not set the odometer. Only odometer readings
     * are baselines for the known odometer; a distance entry is added on top of the latest baseline.
     */
    val odometer: Distance?

    /**
     * The note of the event's entry (`add-event-notes`), shared by every event of that entry, or null when it has none.
     * An entry saved when the vehicle was added never has one.
     */
    val note: String?

    /**
     * The ids of the photos of the event's entry (`add-event-pictures`), shared by every event of that entry, in the order
     * they were attached; empty when it has none. An entry saved when the vehicle was added never has any.
     */
    val photoIds: List<String>

    /** The kinds of event, in the fixed order the events of one entry are listed in, oldest first (`vehicle-log`). */
    enum class Kind(val rank: Int) {
        ODOMETER_READING(0),
        DISTANCE(1),
        REFUELING(2),
    }

    /**
     * The odometer read [reading] at [occurredAt]: it sets the odometer, so it is a baseline for the odometer known at later
     * times. The one in the entry saved when the vehicle was added ([isInitial]) is the vehicle's initial odometer; every
     * other one is an odometer reading logged later (a new odometer count where none was known, or a confirmed lower one).
     */
    data class OdometerReading(
        override val id: String,
        override val occurredAt: ZonedMoment,
        val reading: Distance,
        override val source: EntrySource = EntrySource.LOG_FORM,
        override val note: String? = null,
        override val photoIds: List<String> = emptyList(),
        override val entryId: String = id,
    ) : VehicleEvent {
        override val kind: Kind get() = Kind.ODOMETER_READING
        override val odometer: Distance get() = reading

        /** True for the vehicle's initial odometer, saved when the vehicle was added. */
        val isInitial: Boolean get() = source == EntrySource.VEHICLE_ADDED
    }

    /**
     * A distance driven, logged as a trip distance or as a new odometer count. [distance] is always what was added to the
     * odometer. [loggedOdometer] is the count the user typed when logging by odometer, kept for the log row only: it is
     * never used to derive an odometer.
     */
    data class DistanceEntry(
        override val id: String,
        override val occurredAt: ZonedMoment,
        val distance: Distance,
        val loggedOdometer: Distance? = null,
        override val note: String? = null,
        override val photoIds: List<String> = emptyList(),
        override val entryId: String = id,
    ) : VehicleEvent {
        override val source: EntrySource get() = EntrySource.LOG_FORM
        override val kind: Kind get() = Kind.DISTANCE
        override val odometer: Distance? get() = null
    }

    /**
     * Fuel added (`add-refueling-logging`): an amount, a fuel type and whether the tank was filled up. It never sets or adds
     * to the odometer: a refueling logged with mileage saves that mileage as its entry's own [DistanceEntry] or
     * [OdometerReading]. Every field but [note] and [photoIds] (the entry's) is fixed once saved.
     */
    data class Refueling(
        override val id: String,
        override val occurredAt: ZonedMoment,
        val amount: Volume,
        /** The unit [amount] was entered in: shown in that unit always, never converted — unlike a vehicle's own
         * odometer unit, this is a per-fill-up recording choice, not a fixed vehicle characteristic. */
        val unit: FuelUnit,
        val fuelType: FuelType,
        val filledUp: Boolean,
        override val note: String? = null,
        override val photoIds: List<String> = emptyList(),
        override val entryId: String = id,
    ) : VehicleEvent {
        override val source: EntrySource get() = EntrySource.LOG_FORM
        override val kind: Kind get() = Kind.REFUELING
        override val odometer: Distance? get() = null
    }
}

/** Where an entry was saved from (`atomic-log-events`), stored as a locale-agnostic code. */
enum class EntrySource(val code: String) {
    /** The add-vehicle form: the entry holds the vehicle's initial odometer, and never a note or photos. */
    VEHICLE_ADDED("VEHICLE_ADDED"),

    /** The log event form. */
    LOG_FORM("LOG_FORM"),
    ;

    companion object {
        /** An unknown code (from a newer app version) reads as the log event form. */
        fun fromCode(code: String): EntrySource = entries.firstOrNull { it.code == code } ?: LOG_FORM
    }
}

/**
 * The mileage a refueling is logged with (`add-refueling-logging`): the input to [VehicleRepository.addRefueling], saved as
 * the refueling entry's own event — [Added] as a [VehicleEvent.DistanceEntry], [Anchor] as a [VehicleEvent.OdometerReading].
 */
sealed interface RefuelingMileage {
    /** Added to the odometer. [loggedOdometer] is the count typed, if logged by odometer — for the row only. */
    data class Added(val distance: Distance, val loggedOdometer: Distance? = null) : RefuelingMileage

    /** Sets the odometer. */
    data class Anchor(val reading: Distance) : RefuelingMileage
}

/**
 * Everything one action saved (`atomic-log-events`): its [events] in log order (oldest first), and the [note] and
 * [photoIds] they share. The details and edit screens show an entry, whichever of its events' rows was tapped.
 */
data class LogEntry(
    val id: String,
    val source: EntrySource,
    val events: List<VehicleEvent>,
    val note: String? = null,
    val photoIds: List<String> = emptyList(),
) {
    init {
        require(events.isNotEmpty()) { "An entry holds at least one event" }
    }

    /** The refueling of the entry, when it is one. */
    val refueling: VehicleEvent.Refueling? get() = events.firstNotNullOfOrNull { it as? VehicleEvent.Refueling }

    /** The event a screen shows as the entry's own kind and figure: the refueling when there is one, else its only event. */
    val primary: VehicleEvent get() = refueling ?: events.first()

    /** A refueling's mileage: the entry's odometer reading or distance besides its refueling, or null without one. */
    val mileage: VehicleEvent? get() = if (refueling == null) null else events.firstOrNull { it !is VehicleEvent.Refueling }

    /** Whether the entry's note and photos can be edited: not for the entry saved when the vehicle was added. */
    val isEditable: Boolean get() = source != EntrySource.VEHICLE_ADDED
}

/**
 * The events in log order, oldest first (`vehicle-log`, "Events at the same time are ordered by entry, then by kind"): by
 * instant, then by entry in the order the entries were added, then by [VehicleEvent.Kind.rank] within an entry. The receiver
 * must list the events in the order they were added; an entry's place is where its first event was added.
 */
fun List<VehicleEvent>.inLogOrder(): List<VehicleEvent> {
    val entryOrder = HashMap<String, Int>()
    forEachIndexed { index, event -> entryOrder.getOrPut(event.entryId) { index } }
    return sortedWith(compareBy({ it.occurredAt.instant }, { entryOrder.getValue(it.entryId) }, { it.kind.rank }))
}
