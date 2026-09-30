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

    /** New odometer: the count is not higher than the [known] odometer at the chosen moment. */
    data class OdometerNotHigher(val known: Distance) : LogDistanceError

    /** A refueling's fuel amount field is empty (`add-refueling-logging`); distinct from [FieldEmpty] so the two
     * fields on a refueling form (the fuel amount and its optional mileage) never share one ambiguous error. */
    data object FuelAmountEmpty : LogDistanceError

    /** A refueling's fuel amount of zero; distinct from [DistanceNotPositive] for the same reason. */
    data object FuelAmountNotPositive : LogDistanceError
}

sealed interface LogDistanceResult {
    /** [distance] is what is added to the odometer; [loggedOdometer] is the typed count, only when logging by odometer. */
    data class Valid(val distance: Distance, val loggedOdometer: Distance?) : LogDistanceResult

    /** A new odometer count where no odometer is known: saved as an odometer anchor that sets the odometer to [reading]. */
    data class Anchor(val reading: Distance) : LogDistanceResult

    /**
     * A new odometer count lower than the known odometer (`confirm-lower-odometer`), not yet confirmed. Re-checking with
     * `lowerOdometerConfirmed = true` turns this into [Anchor] instead. An equal count is [Invalid] instead, unaffected.
     */
    data object NeedsLowerOdometerConfirmation : LogDistanceResult

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
 * rules of the way: a trip distance must be above zero; a new odometer must be higher than the known odometer, or, when none is known, becomes an odometer anchor.
 * Wall-clock times are never compared across zones: the future check is on instants.
 *
 * [known] is the previous known odometer at the chosen moment, as `knownOdometerAt` gives it. [mostRecentKnown]
 * (`confirm-lower-odometer`) is the vehicle's actual current odometer (`currentOdometer`), independent of the chosen
 * moment; it defaults to [known] so a caller logging for now — where the two are always equal — needs no extra
 * argument. When they differ, a later event already exists in the log and this entry is backdated into history
 * rather than replacing the vehicle's current odometer. [lowerOdometerConfirmed] (`confirm-lower-odometer`) is true
 * only on a second call, after the user confirmed a lower count than [known] once already asked; it has no effect
 * unless the typed count is actually lower than [known] and [known] equals [mostRecentKnown].
 */
fun validateLogDistance(
    way: LogWay,
    entry: OdometerEntry,
    moment: ZonedMoment,
    now: Instant,
    known: Distance?,
    mostRecentKnown: Distance? = known,
    lowerOdometerConfirmed: Boolean = false,
): LogDistanceResult {
    val typed = entry.toDistance() ?: return LogDistanceResult.Invalid(LogDistanceError.FieldEmpty)
    if (moment.instant > now) return LogDistanceResult.Invalid(LogDistanceError.TimeInFuture)
    return when (way) {
        LogWay.TRIP_DISTANCE ->
            if (typed.meters > 0) LogDistanceResult.Valid(typed, loggedOdometer = null)
            else LogDistanceResult.Invalid(LogDistanceError.DistanceNotPositive)

        LogWay.NEW_ODOMETER -> {
            // Nothing to compare with: the count itself becomes the odometer at that time.
            if (known == null) return LogDistanceResult.Anchor(typed)
            if (typed.meters < known.meters) {
                return when {
                    // A later event already exists: this entry is backdated into history, not replacing the
                    // vehicle's current odometer, so it needs no confirmation.
                    known.meters != mostRecentKnown?.meters -> LogDistanceResult.Anchor(typed)
                    lowerOdometerConfirmed -> LogDistanceResult.Anchor(typed)
                    else -> LogDistanceResult.NeedsLowerOdometerConfirmation
                }
            }
            val distance = distanceByOdometer(typed, known)
                ?: return LogDistanceResult.Invalid(LogDistanceError.OdometerNotHigher(known))
            LogDistanceResult.Valid(distance, loggedOdometer = typed)
        }
    }
}
