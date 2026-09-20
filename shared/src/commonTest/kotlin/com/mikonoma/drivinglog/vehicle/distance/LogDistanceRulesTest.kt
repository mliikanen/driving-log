package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.format.formatOdometer
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class LogDistanceRulesTest {

    private val now = Instant.parse("2026-09-20T13:30:00Z") // 16:30 in Helsinki, 09:30 in New York
    private val past = ZonedMoment(Instant.parse("2026-09-20T10:00:00Z"))

    private fun entry(unit: OdometerUnit, vararg digits: Int) = digits.fold(OdometerEntry(unit)) { e, d -> e.press(d) }

    private fun digitsOf(n: Int): IntArray = n.toString().map { it - '0' }.toIntArray()

    private fun distanceEntry(unit: OdometerUnit, value: Int) = entry(unit, *digitsOf(value))

    private fun validate(
        way: LogWay,
        entry: OdometerEntry,
        moment: ZonedMoment = past,
        known: Distance? = null,
    ) = validateLogDistance(way, entry, moment, now, known)

    private fun valid(result: LogDistanceResult) = result as LogDistanceResult.Valid

    private fun errorOf(result: LogDistanceResult) = (result as LogDistanceResult.Invalid).error

    // Conversion of an entry in every unit combination

    @Test
    fun aKilometerEntryIsConvertedToMeters() {
        assertEquals(Distance(30_000), valid(validate(LogWay.TRIP_DISTANCE, distanceEntry(OdometerUnit.KILOMETERS, 30))).distance)
    }

    @Test
    fun aKilometerEntryWithTenthsIsConvertedToMeters() {
        // 1, 2, 3 in a tenths unit is 12.3 km.
        assertEquals(Distance(12_300), valid(validate(LogWay.TRIP_DISTANCE, entry(OdometerUnit.KILOMETERS_TENTHS, 1, 2, 3))).distance)
    }

    @Test
    fun aMileEntryIsConvertedToMeters() {
        assertEquals(Distance(16_093), valid(validate(LogWay.TRIP_DISTANCE, distanceEntry(OdometerUnit.MILES, 10))).distance)
    }

    @Test
    fun aMileEntryWithTenthsIsConvertedToMeters() {
        // 1, 0, 0 in a tenths unit is 10.0 mi.
        assertEquals(Distance(16_093), valid(validate(LogWay.TRIP_DISTANCE, entry(OdometerUnit.MILES_TENTHS, 1, 0, 0))).distance)
    }

    @Test
    fun tenMilesLoggedForAKilometerWithTenthsVehicleIsShownAs16Point1Km() {
        val distance = valid(validate(LogWay.TRIP_DISTANCE, distanceEntry(OdometerUnit.MILES, 10))).distance
        assertEquals("16.1 km", formatOdometer(distance, OdometerUnit.KILOMETERS_TENTHS, NumberSymbols.ENGLISH_US))
    }

    @Test
    fun twelvePointThreeKmForAWholeNumberVehicleIsShownAs12Km() {
        val distance = valid(validate(LogWay.TRIP_DISTANCE, entry(OdometerUnit.KILOMETERS_TENTHS, 1, 2, 3))).distance
        assertEquals(Distance(12_300), distance)
        assertEquals("12 km", formatOdometer(distance, OdometerUnit.KILOMETERS, NumberSymbols.ENGLISH_US))
    }

    @Test
    fun aTripDistanceHasNoLoggedOdometer() {
        val result = valid(validate(LogWay.TRIP_DISTANCE, distanceEntry(OdometerUnit.KILOMETERS, 30)))
        assertEquals(null, result.loggedOdometer)
    }

    // The distance by odometer

    @Test
    fun theDistanceIsTheCountMinusTheKnownOdometer() {
        val known = Distance(45_230_000)
        val result = valid(validate(LogWay.NEW_ODOMETER, distanceEntry(OdometerUnit.KILOMETERS, 45250), known = known))
        assertEquals(Distance(20_000), result.distance)
        assertEquals(Distance(45_250_000), result.loggedOdometer)
    }

    @Test
    fun theCountCanBeTypedInAnotherUnitThanTheKnownOdometersVehicle() {
        val known = Distance(45_200_000)
        val typed = distanceEntry(OdometerUnit.MILES, 28_100) // 28 100 mi
        val result = valid(validate(LogWay.NEW_ODOMETER, typed, known = known))
        assertEquals(OdometerUnit.MILES.stepsToMeters(28_100) - 45_200_000, result.distance.meters)
    }

    @Test
    fun aTenthsCountGivesATenthsDistance() {
        val known = Distance(45_200_000)
        val typed = entry(OdometerUnit.KILOMETERS_TENTHS, 4, 5, 2, 0, 0, 5) // 45 200.5 km
        assertEquals(Distance(500), valid(validate(LogWay.NEW_ODOMETER, typed, known = known)).distance)
    }

    @Test
    fun distanceByOdometerIsNullWhenTheCountIsNotHigher() {
        assertEquals(null, distanceByOdometer(Distance(100), Distance(100)))
        assertEquals(null, distanceByOdometer(Distance(99), Distance(100)))
        assertEquals(Distance(1), distanceByOdometer(Distance(101), Distance(100)))
    }

    // Validation

    @Test
    fun anEmptyFieldIsRefusedInEitherWay() {
        assertEquals(LogDistanceError.FieldEmpty, errorOf(validate(LogWay.TRIP_DISTANCE, OdometerEntry(OdometerUnit.KILOMETERS))))
        assertEquals(
            LogDistanceError.FieldEmpty,
            errorOf(validate(LogWay.NEW_ODOMETER, OdometerEntry(OdometerUnit.KILOMETERS), known = Distance(1))),
        )
    }

    @Test
    fun aZeroTripDistanceIsRefused() {
        assertEquals(LogDistanceError.DistanceNotPositive, errorOf(validate(LogWay.TRIP_DISTANCE, entry(OdometerUnit.KILOMETERS, 0))))
        assertEquals(LogDistanceError.DistanceNotPositive, errorOf(validate(LogWay.TRIP_DISTANCE, entry(OdometerUnit.KILOMETERS_TENTHS, 0))))
    }

    @Test
    fun aLowerCountIsRefusedNamingTheKnownOdometer() {
        val known = Distance(45_230_000)
        val error = errorOf(validate(LogWay.NEW_ODOMETER, distanceEntry(OdometerUnit.KILOMETERS, 45100), known = known))
        assertEquals(LogDistanceError.OdometerNotHigher(known), error)
    }

    @Test
    fun anEqualCountIsRefused() {
        val known = Distance(45_230_000)
        val error = errorOf(validate(LogWay.NEW_ODOMETER, distanceEntry(OdometerUnit.KILOMETERS, 45230), known = known))
        assertEquals(LogDistanceError.OdometerNotHigher(known), error)
    }

    @Test
    fun aZeroCountIsNotHigherThanAnyKnownOdometer() {
        val error = errorOf(validate(LogWay.NEW_ODOMETER, entry(OdometerUnit.KILOMETERS, 0), known = Distance(0)))
        assertEquals(LogDistanceError.OdometerNotHigher(Distance(0)), error)
    }

    @Test
    fun aNewOdometerWithNoKnownOdometerIsRefused() {
        assertEquals(LogDistanceError.NoKnownOdometer, errorOf(validate(LogWay.NEW_ODOMETER, distanceEntry(OdometerUnit.KILOMETERS, 45250))))
    }

    @Test
    fun aTripDistanceDoesNotNeedAKnownOdometer() {
        valid(validate(LogWay.TRIP_DISTANCE, distanceEntry(OdometerUnit.KILOMETERS, 30), known = null))
    }

    // The order of the rules and the future check

    @Test
    fun anEmptyFieldComesBeforeTheFutureCheck() {
        val future = ZonedMoment(Instant.parse("2026-09-21T00:00:00Z"))
        assertEquals(LogDistanceError.FieldEmpty, errorOf(validate(LogWay.TRIP_DISTANCE, OdometerEntry(OdometerUnit.KILOMETERS), moment = future)))
    }

    @Test
    fun theFutureCheckComesBeforeTheWaysOwnRules() {
        val future = ZonedMoment(Instant.parse("2026-09-21T00:00:00Z"))
        assertEquals(LogDistanceError.TimeInFuture, errorOf(validate(LogWay.TRIP_DISTANCE, entry(OdometerUnit.KILOMETERS, 0), moment = future)))
        assertEquals(
            LogDistanceError.TimeInFuture,
            errorOf(validate(LogWay.NEW_ODOMETER, distanceEntry(OdometerUnit.KILOMETERS, 1), moment = future, known = null)),
        )
    }

    @Test
    fun aMomentExactlyNowIsAccepted() {
        valid(validate(LogWay.TRIP_DISTANCE, distanceEntry(OdometerUnit.KILOMETERS, 1), moment = ZonedMoment(now)))
    }

    @Test
    fun theFutureCheckComparesInstantsNotWallClockTimes() {
        // It is 16:30 in Helsinki and 09:30 in New York.
        val helsinki17 = momentOf(LocalDateTime(2026, 9, 20, 17, 0), "Europe/Helsinki")
        val newYork09 = momentOf(LocalDateTime(2026, 9, 20, 9, 0), "America/New_York")
        val newYork10 = momentOf(LocalDateTime(2026, 9, 20, 10, 0), "America/New_York")
        val km1 = distanceEntry(OdometerUnit.KILOMETERS, 1)

        assertEquals(LogDistanceError.TimeInFuture, errorOf(validate(LogWay.TRIP_DISTANCE, km1, moment = helsinki17)))
        valid(validate(LogWay.TRIP_DISTANCE, km1, moment = newYork09))
        assertEquals(LogDistanceError.TimeInFuture, errorOf(validate(LogWay.TRIP_DISTANCE, km1, moment = newYork10)))
    }

    @Test
    fun theTimeZoneOfAValidEntryIsKeptForTheCaller() {
        val moment = momentOf(LocalDateTime(2026, 9, 20, 8, 30), "America/New_York")
        // The rules do not touch the moment: what is saved is the moment the form built, with its own zone.
        valid(validate(LogWay.TRIP_DISTANCE, distanceEntry(OdometerUnit.KILOMETERS, 5), moment = moment))
        assertEquals("America/New_York", moment.zone?.id)
        assertEquals(TimeZone.of("America/New_York").id, moment.zone?.id)
    }
}
