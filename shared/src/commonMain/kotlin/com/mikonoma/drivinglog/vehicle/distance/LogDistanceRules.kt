package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import kotlin.time.Instant

/** The two ways to log a distance: the distance itself, or the odometer count now. */
enum class LogWay { TRIP_DISTANCE, NEW_ODOMETER }

sealed interface LogDistanceError {
    /** Nothing has been typed in the active field. */
    data object FieldEmpty : LogDistanceError

    /** The chosen moment, as an instant, is later than now. */
    data object TimeInFuture : LogDistanceError

    /** A trip distance of zero. */
    data object DistanceNotPositive : LogDistanceError

    /** New odometer: no odometer-setting event is at or before the chosen moment. */
    data object NoKnownOdometer : LogDistanceError

    /** New odometer: the count is not higher than the [known] odometer at the chosen moment. */
    data class OdometerNotHigher(val known: Distance) : LogDistanceError
}

sealed interface LogDistanceResult {
    /** [distance] is what is added to the odometer; [loggedOdometer] is the typed count, only when logging by odometer. */
    data class Valid(val distance: Distance, val loggedOdometer: Distance?) : LogDistanceResult

    data class Invalid(val error: LogDistanceError) : LogDistanceResult
}

/**
 * The distance a new odometer count means: the count minus the previous known odometer, or null when the count is not
 * higher. Integer meters only, so it is exact whatever the unit the count was typed in.
 */
fun distanceByOdometer(entered: Distance, known: Distance): Distance? =
    if (entered.meters > known.meters) Distance(entered.meters - known.meters) else null

/**
 * Checks a log distance form. The first failing rule wins, in this order: an empty field, a moment in the future, then the
 * rules of the way: a trip distance must be above zero; a new odometer needs a known odometer and must be higher than it.
 * Wall-clock times are never compared across zones: the future check is on instants.
 *
 * [known] is the previous known odometer at the chosen moment, as `knownOdometerAt` gives it.
 */
fun validateLogDistance(
    way: LogWay,
    entry: OdometerEntry,
    moment: ZonedMoment,
    now: Instant,
    known: Distance?,
): LogDistanceResult {
    val typed = entry.toDistance() ?: return LogDistanceResult.Invalid(LogDistanceError.FieldEmpty)
    if (moment.instant > now) return LogDistanceResult.Invalid(LogDistanceError.TimeInFuture)
    return when (way) {
        LogWay.TRIP_DISTANCE ->
            if (typed.meters > 0) LogDistanceResult.Valid(typed, loggedOdometer = null)
            else LogDistanceResult.Invalid(LogDistanceError.DistanceNotPositive)

        LogWay.NEW_ODOMETER -> {
            if (known == null) return LogDistanceResult.Invalid(LogDistanceError.NoKnownOdometer)
            val distance = distanceByOdometer(typed, known)
                ?: return LogDistanceResult.Invalid(LogDistanceError.OdometerNotHigher(known))
            LogDistanceResult.Valid(distance, loggedOdometer = typed)
        }
    }
}
