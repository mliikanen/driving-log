package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.FixedDeviceTimeZone
import com.mikonoma.drivinglog.vehicle.anchorEvent
import com.mikonoma.drivinglog.vehicle.data.FakeClock
import com.mikonoma.drivinglog.vehicle.distanceEvent
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.FuelType
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.RefuelingMileage
import com.mikonoma.drivinglog.vehicle.domain.VehicleFuelType
import com.mikonoma.drivinglog.vehicle.initialEvent
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.fuusio.kide.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class LogEventProcessorTest {

    /** 2026-09-20 13:30 UTC: 16:30 in Helsinki, 09:30 in New York. */
    private val now = Instant.parse("2026-09-20T13:30:00Z")
    private val initialAt = Instant.parse("2026-09-19T12:00:00Z").toEpochMilliseconds()

    private val repository = FakeVehicleRepository()
    private val clock = FakeClock(now)
    private val deviceZone = FixedDeviceTimeZone(TimeZone.of("Europe/Helsinki"))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** A vehicle with an initial odometer of 45 200 km the day before, and a 30 km entry after it. */
    private fun seedVehicle(unit: OdometerUnit = OdometerUnit.KILOMETERS, withEntry: Boolean = true, fuelType: VehicleFuelType = VehicleFuelType.PETROL) {
        repository.seedVehicle("v1", "Family car", unit = unit, fuelType = fuelType)
        val entries = if (withEntry) listOf(distanceEvent("d1", initialAt + 1.hours.inWholeMilliseconds, 30_000)) else emptyList()
        repository.seedEvents("v1", entries + initialEvent("i1", initialAt, 45_200_000))
    }

    private val pictures = com.mikonoma.drivinglog.vehicle.picture.FakePictureStore()
    private val eventPictures = com.mikonoma.drivinglog.vehicle.picture.FakePictureStore()
    private val codec = com.mikonoma.drivinglog.vehicle.picture.FakeImageCodec()
    private val recognizer = com.mikonoma.drivinglog.vehicle.ocr.FakeTextRecognizer()
    private val captures = com.mikonoma.drivinglog.vehicle.ocr.FakeCaptureStore()

    private fun processor(vehicleId: String = "v1") =
        LogEventProcessor(vehicleId, repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)

    private fun LogEventProcessor.type(vararg digits: Int) {
        for (d in digits) dispatch(LogEventIntent.OdometerEdited(state.activeEntry.digits + d))
    }

    private fun LogEventProcessor.error() = state.error

    // Defaults

    @Test
    fun theUnitStartsAsTheVehiclesForEveryUnit() {
        for (unit in OdometerUnit.entries) {
            repository.seedVehicle(unit.name, unit.name, unit = unit)
            repository.seedEvents(unit.name, listOf(initialEvent("i-${unit.name}", initialAt, 1_000)))

            val state = LogEventProcessor(unit.name, repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state

            assertEquals(unit, state.unit, unit.name)
            assertEquals(unit, state.tripDistance.unit, unit.name)
            assertEquals(unit, state.newOdometer.unit, unit.name)
        }
    }

    @Test
    fun theFormStartsOnTripDistanceWithEmptyFields() {
        seedVehicle()
        val state = processor().state

        assertEquals(LogWay.TRIP_DISTANCE, state.way)
        assertTrue(state.tripDistance.isEmpty)
        assertTrue(state.newOdometer.isEmpty)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun theDefaultMomentIsTheTimeTheFormWasOpenedInTheDeviceZone() {
        seedVehicle()
        val processor = processor()

        assertEquals(LocalDateTime(2026, 9, 20, 16, 30), processor.state.localDateTime)
        assertEquals("Europe/Helsinki", processor.state.zoneId)

        // Time passing while the form is open does not move it.
        clock.current = now + 2.hours
        assertEquals(LocalDateTime(2026, 9, 20, 16, 30), processor.state.localDateTime)
    }

    @Test
    fun theDefaultMomentFollowsTheDeviceZoneItIsOpenedIn() {
        seedVehicle()
        deviceZone.zone = TimeZone.of("America/New_York")

        val state = processor().state

        assertEquals(LocalDateTime(2026, 9, 20, 9, 30), state.localDateTime)
        assertEquals("America/New_York", state.zoneId)
    }

    // The two ways

    @Test
    fun eachWayKeepsItsOwnNumber() {
        seedVehicle()
        val processor = processor()
        processor.type(3, 0)
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 5, 2, 5, 0)

        processor.dispatch(LogEventIntent.WayChanged(LogWay.TRIP_DISTANCE))

        assertEquals("30", processor.state.tripDistance.digits)
        assertEquals("45250", processor.state.newOdometer.digits)
        assertEquals("30", processor.state.activeEntry.digits)
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        assertEquals("45250", processor.state.activeEntry.digits)
    }

    // The unit

    @Test
    fun changingTheUnitKeepsTheDigitsInBothFields() {
        seedVehicle()
        val processor = processor()
        processor.type(1, 2, 3)
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 5, 6)

        processor.dispatch(LogEventIntent.UnitFamilySelected(miles = true))

        assertEquals(OdometerUnit.MILES, processor.state.unit)
        assertEquals("123", processor.state.tripDistance.digits)
        assertEquals("456", processor.state.newOdometer.digits)
        assertEquals(OdometerUnit.MILES, processor.state.newOdometer.unit)
    }

    @Test
    fun theTenthsSwitchMakesTheFieldsTenths() {
        seedVehicle()
        val processor = processor()
        processor.type(1, 2, 3)

        processor.dispatch(LogEventIntent.TenthsChanged(included = true))

        assertEquals(OdometerUnit.KILOMETERS_TENTHS, processor.state.unit)
        assertEquals(1230L, processor.state.tripDistance.steps) // 123 km rescaled to 123.0 in tenths
        processor.dispatch(LogEventIntent.TenthsChanged(included = false))
        assertEquals(OdometerUnit.KILOMETERS, processor.state.unit)
        assertEquals(123L, processor.state.tripDistance.steps)
    }

    @Test
    fun everyCombinationOfFamilyAndTenthsIsAUnit() {
        seedVehicle()
        val processor = processor()
        val seen = mutableSetOf<OdometerUnit>()
        for (miles in listOf(false, true)) for (tenths in listOf(false, true)) {
            processor.dispatch(LogEventIntent.UnitFamilySelected(miles))
            processor.dispatch(LogEventIntent.TenthsChanged(tenths))
            seen += processor.state.unit
            assertEquals(miles, processor.state.unit.isMiles)
            assertEquals(tenths, processor.state.unit.hasTenths)
        }
        assertEquals(OdometerUnit.entries.toSet(), seen)
    }

    // The moment

    @Test
    fun changingTheZoneKeepsTheWallClockTimeAndChangesTheInstant() {
        seedVehicle()
        val processor = processor()
        val before = processor.state.moment

        processor.dispatch(LogEventIntent.ZoneChanged("America/New_York"))

        assertEquals(LocalDateTime(2026, 9, 20, 16, 30), processor.state.localDateTime)
        assertEquals("America/New_York", processor.state.zoneId)
        assertEquals(before.localDateTime, processor.state.moment.localDateTime)
        assertTrue(processor.state.moment.instant != before.instant)
    }

    @Test
    fun theDateAndTheTimeCanBeChanged() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 19)))
        processor.dispatch(LogEventIntent.TimeChanged(8, 5))

        assertEquals(LocalDateTime(2026, 9, 19, 8, 5), processor.state.localDateTime)
    }

    // Validation

    // disable-invalid-save: the screen now disables Save while the active field is empty, so this exercises the
    // processor directly (as every test here does), a defense-in-depth check behind the UI gate rather than the
    // primary coverage of "empty is refused".
    @Test
    fun anEmptyFieldIsRefusedInBothWays() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.Save)
        assertEquals(LogDistanceError.FieldEmpty, processor.error())

        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.dispatch(LogEventIntent.Save)
        assertEquals(LogDistanceError.FieldEmpty, processor.error())
        assertEquals(emptyList(), repository.distanceCalls)
    }

    @Test
    fun aZeroTripDistanceIsRefused() {
        seedVehicle()
        val processor = processor()
        processor.type(0)

        processor.dispatch(LogEventIntent.Save)

        assertEquals(LogDistanceError.DistanceNotPositive, processor.error())
        assertEquals(emptyList(), repository.distanceCalls)
    }

    @Test
    fun anEqualCountIsRefusedNamingTheKnownOdometer() {
        seedVehicle() // known odometer now: 45 200 km + 30 km
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))

        processor.type(4, 5, 2, 3, 0)
        processor.dispatch(LogEventIntent.Save)
        assertEquals(LogDistanceError.OdometerNotHigher(Distance(45_230_000)), processor.error())
        assertEquals(emptyList(), repository.distanceCalls)
        assertFalse(processor.state.lowerOdometerConfirmationPending)
    }

    @Test
    fun aLowerCountAsksForConfirmationInsteadOfSaving() {
        seedVehicle() // known odometer now: 45 200 km + 30 km
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))

        processor.type(4, 5, 1, 0, 0)
        processor.dispatch(LogEventIntent.Save)

        assertTrue(processor.state.lowerOdometerConfirmationPending)
        assertNull(processor.error())
        assertEquals(emptyList(), repository.anchorCalls)
    }

    @Test
    fun confirmingALowerCountSavesItAsAnAnchorAndClearsThePending() = runTest {
        seedVehicle() // known odometer now: 45 200 km + 30 km
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 5, 1, 0, 0)
        processor.dispatch(LogEventIntent.Save)
        assertTrue(processor.state.lowerOdometerConfirmationPending)

        processor.test {
            dispatch(LogEventIntent.LowerOdometerConfirmed)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertFalse(processor.state.lowerOdometerConfirmationPending)
        val call = repository.anchorCalls.single()
        assertEquals(Distance(45_100_000), call.reading)
    }

    @Test
    fun cancellingALowerCountSavesNothingAndLeavesTheTypedFieldUntouched() {
        seedVehicle() // known odometer now: 45 200 km + 30 km
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 5, 1, 0, 0)
        processor.dispatch(LogEventIntent.Save)
        assertTrue(processor.state.lowerOdometerConfirmationPending)

        processor.dispatch(LogEventIntent.LowerOdometerCancelled)

        assertFalse(processor.state.lowerOdometerConfirmationPending)
        assertEquals(emptyList(), repository.anchorCalls)
        assertEquals(emptyList(), repository.distanceCalls)
        assertEquals("45100", processor.state.newOdometer.digits)
    }

    @Test
    fun aBackdatedLowerCountIsSavedDirectlyWithoutConfirmation() = runTest {
        seedVehicle() // initial odometer 45,200 km at initialAt; a +30 km entry an hour later (current odometer 45,230 km)
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 19)))
        processor.dispatch(LogEventIntent.TimeChanged(15, 30)) // between the initial odometer and the later +30 km entry
        assertEquals(Distance(45_200_000), processor.state.knownOdometer)

        processor.type(4, 4, 0, 0, 0) // 44 000 km: lower than the known-at-moment odometer, but not the vehicle's current one

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertFalse(processor.state.lowerOdometerConfirmationPending)
        val call = repository.anchorCalls.single()
        assertEquals(Distance(44_000_000), call.reading)
        // The later +30 km entry chains forward from the new anchor, exactly as it would from a higher backdated
        // anchor (`vehicle-log`, "A later anchor replaces the running total") — this change doesn't alter that.
        assertEquals(Distance(44_030_000), repository.observeVehicleOdometer("v1"))
    }

    @Test
    fun aMomentInTheFutureIsRefused() {
        seedVehicle()
        val processor = processor()
        processor.type(1, 0)
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 21)))

        processor.dispatch(LogEventIntent.Save)

        assertEquals(LogDistanceError.TimeInFuture, processor.error())
        assertEquals(emptyList(), repository.distanceCalls)
    }

    @Test
    fun theFutureCheckIsOnTheInstantInTheChosenZone() {
        seedVehicle()
        val processor = processor()
        processor.type(1)
        // 16:30 Helsinki now (09:30 New York). 10:00 New York has not happened; 09:00 New York has.
        processor.dispatch(LogEventIntent.ZoneChanged("America/New_York"))
        processor.dispatch(LogEventIntent.TimeChanged(10, 0))
        processor.dispatch(LogEventIntent.Save)
        assertEquals(LogDistanceError.TimeInFuture, processor.error())

        processor.dispatch(LogEventIntent.TimeChanged(9, 0))
        assertNull(processor.error())
        processor.dispatch(LogEventIntent.Save)
        assertNull(processor.error())
        assertEquals(1, repository.distanceCalls.size)
    }

    @Test
    fun aNewOdometerBeforeTheInitialOdometerIsSavedAsAnAnchor() = runTest {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 4, 0, 0, 0)
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 12)))

        assertNull(processor.state.knownOdometer)
        assertNull(processor.state.previewDistance)
        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertNull(processor.error())
        assertEquals(emptyList(), repository.distanceCalls)
        val call = repository.anchorCalls.single()
        assertEquals(Distance(44_000_000), call.reading)
        // 16:30 Helsinki (UTC+3) on the 12th.
        assertEquals(Instant.parse("2026-09-12T13:30:00Z"), call.occurredAt.instant)
        assertEquals("Europe/Helsinki", call.occurredAt.zone?.id)
        assertFalse(call.tenthsIncluded)
    }

    @Test
    fun anAnchorSavedWithTenthsRemembersThem() = runTest {
        seedVehicle(OdometerUnit.KILOMETERS_TENTHS)
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 4, 0, 0, 0, 5)
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 12)))

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val call = repository.anchorCalls.single()
        assertEquals(Distance(44_000_500), call.reading)
        assertTrue(call.tenthsIncluded)
    }

    @Test
    fun anAnchorWithNothingTypedIsRefused() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 12)))

        processor.dispatch(LogEventIntent.Save)

        assertEquals(LogDistanceError.FieldEmpty, processor.error())
        assertEquals(emptyList(), repository.anchorCalls)
    }

    @Test
    fun aFailedAnchorSaveLetsTheUserTryAgain() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 4, 0, 0, 0)
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 12)))
        repository.distanceFailure = IllegalStateException("disk full")

        runCatching { processor.dispatch(LogEventIntent.Save) }

        assertFalse(processor.state.isSaving)
        assertEquals(emptyList(), repository.anchorCalls)
    }

    @Test
    fun anAnchorIsTheKnownOdometerForLaterTimesBeforeTheInitialOdometer() {
        seedVehicle()
        // An anchor of 44 000 km a week before the initial odometer, and a 30 km trip three days before it.
        val weekBefore = initialAt - 7 * 24.hours.inWholeMilliseconds
        repository.seedEvents(
            "v1",
            // Newest first.
            listOf(
                initialEvent("i1", initialAt, 45_200_000),
                distanceEvent("d0", initialAt - 3 * 24.hours.inWholeMilliseconds, 30_000),
                anchorEvent("a0", weekBefore, 44_000_000),
            ),
        )
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 17))) // 16:30 Helsinki, after both

        assertEquals(Distance(44_030_000), processor.state.knownOdometer)

        // Before the anchor nothing is known again.
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 1)))
        assertNull(processor.state.knownOdometer)
    }

    @Test
    fun aTripDistanceBeforeTheInitialOdometerIsAccepted() = runTest {
        seedVehicle()
        val processor = processor()
        processor.type(3, 0)
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 12)))

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(1, repository.distanceCalls.size)
        assertEquals(Distance(30_000), repository.distanceCalls.single().distance)
        // The entry is in the log at its earlier time and the current odometer is unchanged.
        assertEquals(Distance(45_230_000), repository.observeVehicleOdometer("v1"))
    }

    // Errors clear

    @Test
    fun anErrorClearsWhenTheUserTypes() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.Save)
        assertEquals(LogDistanceError.FieldEmpty, processor.error())

        processor.type(5)

        assertNull(processor.error())
    }

    @Test
    fun anErrorStaysWhileTheFieldIsCleared() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.Save)

        processor.dispatch(LogEventIntent.OdometerCleared)

        assertEquals(LogDistanceError.FieldEmpty, processor.error())
    }

    @Test
    fun anErrorClearsWhenTheTimeTheZoneTheWayOrTheUnitChanges() {
        seedVehicle()
        val processor = processor()
        val changes = listOf<() -> Unit>(
            { processor.dispatch(LogEventIntent.TimeChanged(10, 0)) },
            { processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 19))) },
            { processor.dispatch(LogEventIntent.ZoneChanged("Asia/Tokyo")) },
            { processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER)) },
            { processor.dispatch(LogEventIntent.UnitFamilySelected(miles = true)) },
            { processor.dispatch(LogEventIntent.TenthsChanged(included = true)) },
        )
        for (change in changes) {
            processor.dispatch(LogEventIntent.Save)
            assertEquals(LogDistanceError.FieldEmpty, processor.error())
            change()
            assertNull(processor.error())
        }
    }

    // The known odometer and the live distance

    @Test
    fun theKnownOdometerIncludesEarlierEntries() {
        seedVehicle()
        assertEquals(Distance(45_230_000), processor().state.knownOdometer)
    }

    @Test
    fun theKnownOdometerFollowsTheChosenTime() {
        seedVehicle()
        val processor = processor()
        // The entry of 30 km is one hour after the initial event, at 13:00 UTC on the 19th (16:00 Helsinki).
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 19)))
        processor.dispatch(LogEventIntent.TimeChanged(15, 0))
        assertEquals(Distance(45_200_000), processor.state.knownOdometer)

        processor.dispatch(LogEventIntent.TimeChanged(16, 30))
        assertEquals(Distance(45_230_000), processor.state.knownOdometer)
    }

    @Test
    fun theLiveDistanceFollowsTheTypedCount() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        assertNull(processor.state.previewDistance)

        processor.type(4, 5, 2, 5, 0)
        assertEquals(Distance(20_000), processor.state.previewDistance)

        processor.dispatch(LogEventIntent.OdometerCleared)
        processor.type(4, 5, 1, 0, 0)
        assertNull(processor.state.previewDistance)
    }

    @Test
    fun theLogFollowsTheRepository() {
        seedVehicle(withEntry = false)
        val processor = processor()
        assertEquals(Distance(45_200_000), processor.state.knownOdometer)

        repository.seedEvents("v1", listOf(distanceEvent("d9", initialAt + 2.hours.inWholeMilliseconds, 5_000), initialEvent("i1", initialAt, 45_200_000)))

        assertEquals(Distance(45_205_000), processor.state.knownOdometer)
    }

    // Saving

    @Test
    fun aSavedTripDistanceAddsOneEntryWithTheChosenZone() = runTest {
        seedVehicle()
        val processor = processor()
        processor.type(1, 2)
        processor.dispatch(LogEventIntent.ZoneChanged("America/New_York"))
        processor.dispatch(LogEventIntent.TimeChanged(8, 30))

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val call = repository.distanceCalls.single()
        assertEquals("v1", call.vehicleId)
        assertEquals(Distance(12_000), call.distance)
        assertNull(call.loggedOdometer)
        assertEquals("America/New_York", call.occurredAt.zone?.id)
        assertEquals(-4 * 3600, call.occurredAt.zone?.offsetSeconds)
        assertEquals(Instant.parse("2026-09-20T12:30:00Z"), call.occurredAt.instant)
    }

    @Test
    fun aSavedNewOdometerAddsTheCalculatedDistanceAndTheTypedCount() = runTest {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 5, 2, 5, 0)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val call = repository.distanceCalls.single()
        assertEquals(Distance(20_000), call.distance)
        assertEquals(Distance(45_250_000), call.loggedOdometer)
    }

    @Test
    fun anEntryInAnotherUnitIsConvertedToMeters() = runTest {
        seedVehicle(OdometerUnit.KILOMETERS_TENTHS)
        val processor = processor()
        processor.dispatch(LogEventIntent.UnitFamilySelected(miles = true))
        processor.dispatch(LogEventIntent.TenthsChanged(included = false))
        processor.type(1, 0)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(Distance(16_093), repository.distanceCalls.single().distance)
    }

    @Test
    fun aSecondSaveWhileSavingIsIgnoredAndAFailedSaveCanBeRetried() {
        seedVehicle()
        val processor = processor()
        processor.type(5)
        repository.distanceFailure = IllegalStateException("disk full")

        processor.dispatch(LogEventIntent.Save)

        assertFalse(processor.state.isSaving)
        assertEquals(emptyList(), repository.distanceCalls)
        repository.distanceFailure = null
        processor.dispatch(LogEventIntent.Save)
        assertEquals(1, repository.distanceCalls.size)
    }

    @Test
    fun aMissingVehicleIsReportedAndCannotBeSaved() {
        val processor = LogEventProcessor("missing", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)

        assertTrue(processor.state.notFound)
        processor.type(5)
        processor.dispatch(LogEventIntent.Save)

        assertEquals(emptyList(), repository.distanceCalls)
    }

    // Restoring after process death

    @Test
    fun aRestoredFormKeepsWhatTheUserChoseAndTheRepositoryDataArrivesAfterwards() = runTest {
        // Like the app: the restore happens right after construction, and the repository data arrives later.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedVehicle()
        val saved = LogEventProcessor("v1", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).let { first ->
            advanceUntilIdle()
            first.dispatch(LogEventIntent.UnitFamilySelected(miles = true))
            first.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
            first.dispatch(LogEventIntent.ZoneChanged("Asia/Tokyo"))
            advanceUntilIdle()
            first.dispatch(LogEventIntent.OdometerEdited("123"))
            advanceUntilIdle()
            checkNotNull(first.stateToSave())
        }

        val restored = LogEventProcessor("v1", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)
        restored.restoreState(saved)
        advanceUntilIdle()

        assertEquals(OdometerUnit.MILES, restored.state.unit)
        assertEquals(LogWay.NEW_ODOMETER, restored.state.way)
        assertEquals("123", restored.state.newOdometer.digits)
        assertEquals("Asia/Tokyo", restored.state.zoneId)
        assertEquals(OdometerUnit.KILOMETERS, restored.state.vehicleUnit) // from the repository, loaded after the restore
        assertFalse(restored.state.isLoading)
        assertEquals(2, restored.state.log.size)
    }

    // ---- The kind of event (Distance and Refueling; add-refueling-logging)

    @Test
    fun theFormStartsWithTheDistanceKind() {
        seedVehicle()

        assertEquals(LogKind.DISTANCE, processor().state.kind)
    }

    @Test
    fun choosingTheSameKindLeavesTheFormAsItIs() {
        seedVehicle()
        val processor = processor()
        processor.type(3, 0)

        processor.dispatch(LogEventIntent.KindSelected(LogKind.DISTANCE))

        assertEquals(LogKind.DISTANCE, processor.state.kind)
        assertEquals("30", processor.state.tripDistance.digits)
    }

    @Test
    fun choosingRefuelingSwitchesTheFormToItsFields() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))

        assertEquals(LogKind.REFUELING, processor.state.kind)
        assertTrue(processor.state.fuelAmount.isEmpty)
    }

    @Test
    fun choosingAKindClearsAnExistingError() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.Save)
        assertNotNull(processor.error())

        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))

        assertNull(processor.error())
    }

    @Test
    fun switchingKindsKeepsWhatWasTypedInEach() {
        seedVehicle()
        val processor = processor()
        processor.type(3, 0)

        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.dispatch(LogEventIntent.FuelAmountEdited("4230"))
        processor.dispatch(LogEventIntent.KindSelected(LogKind.DISTANCE))

        assertEquals("30", processor.state.tripDistance.digits)
        assertEquals(LogKind.DISTANCE, processor.state.kind)
    }

    @Test
    fun theKindSurvivesRestoreState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedVehicle()
        val saved = processor().let { first ->
            advanceUntilIdle()
            checkNotNull(first.stateToSave())
        }

        val restored = processor()
        restored.restoreState(saved)
        advanceUntilIdle()

        assertEquals(LogKind.DISTANCE, restored.state.kind)
    }

    // ---- The note (add-event-notes)

    @Test
    fun theFormStartsWithNoPendingNoteAndTheEditorClosed() {
        seedVehicle()

        val state = processor().state

        assertNull(state.pendingNote)
        assertNull(state.noteDraft)
    }

    @Test
    fun openingTheEditorSeedsTheDraftFromNothingWhenNoNoteIsPending() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.NoteEditorOpened)

        assertEquals("", processor.state.noteDraft)
    }

    @Test
    fun openingTheEditorSeedsTheDraftFromThePendingNote() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("borrowed to Sam"))
        processor.dispatch(LogEventIntent.NoteAttached)

        processor.dispatch(LogEventIntent.NoteEditorOpened)

        assertEquals("borrowed to Sam", processor.state.noteDraft)
    }

    @Test
    fun attachingStoresTheTrimmedTextAndClosesTheEditor() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.NoteEditorOpened)

        processor.dispatch(LogEventIntent.NoteDraftEdited("  borrowed to Sam  "))
        processor.dispatch(LogEventIntent.NoteAttached)

        assertEquals("borrowed to Sam", processor.state.pendingNote)
        assertNull(processor.state.noteDraft)
    }

    @Test
    fun attachingBlankTextClearsTheNote() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("a note"))
        processor.dispatch(LogEventIntent.NoteAttached)

        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("   "))
        processor.dispatch(LogEventIntent.NoteAttached)

        assertNull(processor.state.pendingNote)
    }

    @Test
    fun discardingDropsTheDraftButKeepsThePreviousNote() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("a note"))
        processor.dispatch(LogEventIntent.NoteAttached)

        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("something else entirely"))
        processor.dispatch(LogEventIntent.NoteDiscarded)

        assertEquals("a note", processor.state.pendingNote)
        assertNull(processor.state.noteDraft)
    }

    @Test
    fun removingAfterConfirmationClearsTheNote() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("a note"))
        processor.dispatch(LogEventIntent.NoteAttached)

        processor.dispatch(LogEventIntent.NoteRemoveRequested)
        assertTrue(processor.state.noteRemovalPending)
        processor.dispatch(LogEventIntent.NoteRemoveConfirmed)

        assertNull(processor.state.pendingNote)
        assertFalse(processor.state.noteRemovalPending)
    }

    @Test
    fun cancellingRemovalKeepsTheNote() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("a note"))
        processor.dispatch(LogEventIntent.NoteAttached)

        processor.dispatch(LogEventIntent.NoteRemoveRequested)
        processor.dispatch(LogEventIntent.NoteRemoveCancelled)

        assertEquals("a note", processor.state.pendingNote)
        assertFalse(processor.state.noteRemovalPending)
    }

    @Test
    fun theNoteAndTheOpenEditorSurviveRestoreState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedVehicle()
        val saved = processor().let { first ->
            advanceUntilIdle()
            first.dispatch(LogEventIntent.NoteEditorOpened)
            first.dispatch(LogEventIntent.NoteDraftEdited("still typing"))
            advanceUntilIdle()
            checkNotNull(first.stateToSave())
        }

        val restored = processor()
        restored.restoreState(saved)
        advanceUntilIdle()

        assertEquals("still typing", restored.state.noteDraft)
        assertNull(restored.state.pendingNote)
    }

    @Test
    fun savingWithAPendingNoteReachesTheRepository() = runTest {
        seedVehicle()
        val processor = processor()
        processor.type(1, 2)
        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("borrowed to Sam"))
        processor.dispatch(LogEventIntent.NoteAttached)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals("borrowed to Sam", repository.distanceCalls.single().note)
    }

    @Test
    fun savingWithoutANoteReachesTheRepositoryWithNull() = runTest {
        seedVehicle()
        val processor = processor()
        processor.type(1, 2)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertNull(repository.distanceCalls.single().note)
    }

    // ---- Photos (add-event-pictures)

    private fun LogEventProcessor.pick() = dispatch(LogEventIntent.PhotoPicked(PhotoResult.Chosen(byteArrayOf(1))))

    @Test
    fun theFormStartsWithNoPhotosAttached() {
        seedVehicle()

        val state = processor().state

        assertEquals(emptyList(), state.photos.pendingIds)
        assertEquals(emptyList(), state.photoPreviewUris)
    }

    @Test
    fun pickingAPhotoAddsItToTheStrip() {
        seedVehicle()
        val processor = processor()

        processor.pick()

        assertEquals(1, processor.state.photos.pendingIds.size)
        assertEquals(1, processor.state.photoPreviewUris.size)
        assertNull(processor.state.photos.error)
    }

    @Test
    fun theLimitIsReachedAfterFivePhotos() {
        seedVehicle()
        val processor = processor()
        repeat(5) { processor.pick() }

        assertTrue(processor.state.photos.isFull)
        assertEquals(5, processor.state.photos.pendingIds.size)

        processor.pick() // a 6th pick while full changes nothing

        assertEquals(5, processor.state.photos.pendingIds.size)
    }

    @Test
    fun removingOneAllowsAnotherAfterTheCap() {
        seedVehicle()
        val processor = processor()
        repeat(5) { processor.pick() }
        val firstId = processor.state.photos.pendingIds.first()

        processor.dispatch(LogEventIntent.PhotoRemoveRequested(firstId))
        processor.dispatch(LogEventIntent.PhotoRemoveConfirmed)
        assertFalse(processor.state.photos.isFull)

        processor.pick()

        assertEquals(5, processor.state.photos.pendingIds.size)
    }

    @Test
    fun pickingAnUnreadablePhotoSetsAnErrorAndAddsNothing() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.PhotoPicked(PhotoResult.Unreadable))

        assertEquals(PictureError.COULD_NOT_OPEN, processor.state.photos.error)
        assertEquals(emptyList(), processor.state.photos.pendingIds)
    }

    @Test
    fun removingAPhotoAsksForConfirmationFirst() {
        seedVehicle()
        val processor = processor()
        processor.pick()
        val id = processor.state.photos.pendingIds.single()

        processor.dispatch(LogEventIntent.PhotoRemoveRequested(id))

        assertEquals(id, processor.state.photos.removalPendingId)
        assertEquals(listOf(id), processor.state.photos.pendingIds)
    }

    @Test
    fun confirmingRemovalDropsThePhoto() {
        seedVehicle()
        val processor = processor()
        processor.pick()
        val id = processor.state.photos.pendingIds.single()
        processor.dispatch(LogEventIntent.PhotoRemoveRequested(id))

        processor.dispatch(LogEventIntent.PhotoRemoveConfirmed)

        assertEquals(emptyList(), processor.state.photos.pendingIds)
        assertNull(processor.state.photos.removalPendingId)
        assertEquals(emptyList(), processor.state.photoPreviewUris)
    }

    @Test
    fun cancellingRemovalKeepsThePhoto() {
        seedVehicle()
        val processor = processor()
        processor.pick()
        val id = processor.state.photos.pendingIds.single()
        processor.dispatch(LogEventIntent.PhotoRemoveRequested(id))

        processor.dispatch(LogEventIntent.PhotoRemoveCancelled)

        assertEquals(listOf(id), processor.state.photos.pendingIds)
        assertNull(processor.state.photos.removalPendingId)
    }

    @Test
    fun leavingWithoutSavingDiscardsThePendingPhotoFiles() {
        seedVehicle()
        val processor = processor()
        processor.pick()
        val id = processor.state.photos.pendingIds.single()
        assertTrue(id in eventPictures.pending)

        processor.dispatch(LogEventIntent.Left)

        assertEquals(false, id in eventPictures.pending)
        assertEquals(emptyList(), repository.distanceCalls)
    }

    @Test
    fun savingWithPhotosReachesTheRepository() = runTest {
        seedVehicle()
        val processor = processor()
        processor.type(1, 2)
        processor.pick()
        processor.pick()

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(2, repository.distanceCalls.single().photos.size)
    }

    @Test
    fun savingWithoutPhotosReachesTheRepositoryWithNone() = runTest {
        seedVehicle()
        val processor = processor()
        processor.type(1, 2)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(emptyList(), repository.distanceCalls.single().photos)
    }

    @Test
    fun photosSurviveRestoreState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedVehicle()
        val saved = LogEventProcessor("v1", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).let { first ->
            advanceUntilIdle()
            first.dispatch(LogEventIntent.PhotoPicked(PhotoResult.Chosen(byteArrayOf(1))))
            advanceUntilIdle()
            checkNotNull(first.stateToSave())
        }

        val restored = LogEventProcessor("v1", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)
        restored.restoreState(saved)
        advanceUntilIdle()

        assertEquals(1, restored.state.photos.pendingIds.size)
        // The screen always dispatches this once on open (LogEventScreen.kt), so the strip's thumbnail is never left
        // stale even where a real platform restore does drop transient fields the in-memory restoreState() used
        // above does not.
        restored.dispatch(LogEventIntent.PhotoPreviewRefresh)
        advanceUntilIdle()
        assertEquals(1, restored.state.photoPreviewUris.size)
    }

    // ---- Choosing a vehicle (opened from the Home screen: an empty vehicle id)

    private fun chooser() = LogEventProcessor("", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)

    @Test
    fun withNothingRememberedTheChooserStartsOnTheFirstVehicleByName() {
        seedRemembered("van", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("Bike", OdometerUnit.KILOMETERS, remembered = null)

        val processor = chooser()

        assertEquals("Bike", processor.state.selectedVehicleId)
        assertEquals(listOf("Bike", "van"), processor.state.vehicles.map { it.id })
    }

    @Test
    fun theChooserStartsOnTheVehicleLastLoggedFor() {
        seedRemembered("van", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("Bike", OdometerUnit.KILOMETERS, remembered = null)
        repository.seedLastLoggedVehicleId("van")

        assertEquals("van", chooser().state.selectedVehicleId)
    }

    @Test
    fun aRememberedVehicleThatIsGoneFallsBackToTheFirstByName() {
        seedRemembered("van", OdometerUnit.KILOMETERS, remembered = null)
        repository.seedLastLoggedVehicleId("gone")

        assertEquals("van", chooser().state.selectedVehicleId)
    }

    @Test
    fun aRestoredChoiceThatStillExistsIsKept() = runTest {
        seedRemembered("van", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("Bike", OdometerUnit.KILOMETERS, remembered = null)
        repository.seedLastLoggedVehicleId("van")
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val saved = chooser().let { first ->
            advanceUntilIdle()
            first.dispatch(LogEventIntent.VehicleSelected("Bike"))
            advanceUntilIdle()
            checkNotNull(first.stateToSave())
        }

        val restored = chooser()
        restored.restoreState(saved)
        advanceUntilIdle()

        // Not "van" (the remembered one): the restored choice, "Bike", is kept.
        assertEquals("Bike", restored.state.selectedVehicleId)
    }

    @Test
    fun choosingAVehicleSwitchesTheKnownOdometerAndTheLog() {
        seedRemembered("van", OdometerUnit.KILOMETERS, remembered = null)
        repository.seedVehicle("bike", "Bike", unit = OdometerUnit.MILES)
        repository.seedEvents("bike", listOf(initialEvent("i-bike", initialAt, 1_000_000)))
        val processor = chooser()

        processor.dispatch(LogEventIntent.VehicleSelected("bike"))

        assertEquals("bike", processor.state.selectedVehicleId)
        assertEquals(Distance(1_000_000), processor.state.knownOdometer)
        assertEquals(1, processor.state.log.size)
    }

    @Test
    fun choosingAVehicleConvertsTheTypedDigitsToItsUnit() {
        // Named so "aaa-van" sorts first: the chooser starts on it.
        seedRemembered("aaa-van", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("zzz-bike", OdometerUnit.MILES, remembered = null)
        val processor = chooser()
        assertEquals("aaa-van", processor.state.selectedVehicleId)
        processor.type(1, 2, 3) // 123 (a whole-number unit)

        processor.dispatch(LogEventIntent.VehicleSelected("zzz-bike"))

        // The unit changed from kilometers to miles; the digits are kept and converted, as a manual unit change would.
        assertEquals(OdometerUnit.MILES, processor.state.unit)
        assertEquals("123", processor.state.tripDistance.digits)
    }

    @Test
    fun choosingAnotherVehicleKeepsThePendingNote() {
        // distance-logging, "A pending note survives a vehicle change".
        seedRemembered("aaa-van", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("zzz-bike", OdometerUnit.MILES, remembered = null)
        val processor = chooser()
        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("a note"))
        processor.dispatch(LogEventIntent.NoteAttached)

        processor.dispatch(LogEventIntent.VehicleSelected("zzz-bike"))

        assertEquals("a note", processor.state.pendingNote)
    }

    @Test
    fun choosingAnUnknownVehicleChangesNothing() {
        seedRemembered("van", OdometerUnit.KILOMETERS, remembered = null)
        val processor = chooser()

        processor.dispatch(LogEventIntent.VehicleSelected("not-a-vehicle"))

        assertEquals("van", processor.state.selectedVehicleId)
    }

    @Test
    fun savingAddsTheEntryToTheChosenVehicle() = runTest {
        seedRemembered("van", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("bike", OdometerUnit.KILOMETERS, remembered = null)
        val processor = chooser()
        processor.dispatch(LogEventIntent.VehicleSelected("bike"))
        processor.type(5)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals("bike", repository.distanceCalls.single().vehicleId)
    }

    @Test
    fun theChosenVehicleSurvivesRestoreState() = runTest {
        seedRemembered("van", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("bike", OdometerUnit.KILOMETERS, remembered = null)
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val saved = chooser().let { first ->
            advanceUntilIdle()
            first.dispatch(LogEventIntent.VehicleSelected("bike"))
            checkNotNull(first.stateToSave())
        }

        val restored = chooser()
        restored.restoreState(saved)
        advanceUntilIdle()

        assertEquals("bike", restored.state.selectedVehicleId)
        assertEquals(OdometerUnit.KILOMETERS, restored.state.vehicleUnit)
    }

    @Test
    fun theDetailsRouteHasNoSelectionAndBehavesAsBefore() {
        seedVehicle()

        val processor = processor()

        assertEquals(emptyList(), processor.state.vehicles)
        assertEquals("v1", processor.state.selectedVehicleId)
        processor.dispatch(LogEventIntent.VehicleSelected("v1")) // has no selector; a stray intent changes nothing
        assertEquals("v1", processor.state.selectedVehicleId)
    }

    // ---- The tenths choice remembered per vehicle

    private fun seedRemembered(id: String, unit: OdometerUnit, remembered: Boolean?) {
        repository.seedVehicle(id, id, unit = unit, logDistanceTenths = remembered)
        repository.seedEvents(id, listOf(initialEvent("i-$id", initialAt, 45_200_000)))
    }

    @Test
    fun theTenthsChoiceStartsAsTheRememberedOne() {
        seedRemembered("a", OdometerUnit.KILOMETERS, remembered = true)
        seedRemembered("b", OdometerUnit.MILES_TENTHS, remembered = false)

        assertEquals(OdometerUnit.KILOMETERS_TENTHS, LogEventProcessor("a", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
        assertEquals(OdometerUnit.MILES, LogEventProcessor("b", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
    }

    @Test
    fun withNoRememberedChoiceTheTenthsOfTheVehiclesUnitAreUsed() {
        seedRemembered("a", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("b", OdometerUnit.KILOMETERS_TENTHS, remembered = null)

        assertEquals(OdometerUnit.KILOMETERS, LogEventProcessor("a", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, LogEventProcessor("b", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
    }

    @Test
    fun onlyTheTenthsChoiceIsRememberedNotTheUnitFamily() {
        // A miles vehicle whose remembered choice is "without tenths" starts in miles, and a kilometer vehicle keeps kilometers.
        seedRemembered("m", OdometerUnit.MILES, remembered = true)
        seedRemembered("k", OdometerUnit.KILOMETERS, remembered = true)

        assertEquals(OdometerUnit.MILES_TENTHS, LogEventProcessor("m", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, LogEventProcessor("k", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
    }

    @Test
    fun savingHandsTheTenthsChoiceUsedToTheRepository() = runTest {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.TenthsChanged(included = true))
        processor.type(1, 2, 3)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(true, repository.distanceCalls.single().tenthsIncluded)
    }

    @Test
    fun savingWithoutTenthsHandsFalseEvenForAVehicleWithTenths() = runTest {
        seedVehicle(OdometerUnit.KILOMETERS_TENTHS)
        val processor = processor()
        processor.dispatch(LogEventIntent.TenthsChanged(included = false))
        processor.type(5)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(false, repository.distanceCalls.single().tenthsIncluded)
    }

    @Test
    fun theNextFormForTheVehicleStartsWithTheChoiceThatWasSaved() = runTest {
        seedVehicle()
        val first = processor()
        first.dispatch(LogEventIntent.TenthsChanged(included = true))
        first.type(1, 2, 3)
        first.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val next = processor()

        assertEquals(OdometerUnit.KILOMETERS_TENTHS, next.state.unit)
        assertTrue(next.state.tripDistance.isEmpty)
    }

    @Test
    fun aChoiceThatIsNotSavedIsNotRemembered() {
        seedRemembered("a", OdometerUnit.KILOMETERS, remembered = false)
        val left = LogEventProcessor("a", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)
        left.dispatch(LogEventIntent.TenthsChanged(included = true))
        left.type(1, 2)
        // The form is left without saving.

        val next = LogEventProcessor("a", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)

        assertEquals(OdometerUnit.KILOMETERS, next.state.unit)
        assertEquals(emptyList(), repository.distanceCalls)
    }

    @Test
    fun aRefusedSaveDoesNotRememberTheChoice() {
        seedRemembered("a", OdometerUnit.KILOMETERS, remembered = null)
        val processor = LogEventProcessor("a", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)
        processor.dispatch(LogEventIntent.TenthsChanged(included = true))

        processor.dispatch(LogEventIntent.Save) // nothing typed

        assertEquals(LogDistanceError.FieldEmpty, processor.state.error)
        assertEquals(OdometerUnit.KILOMETERS, LogEventProcessor("a", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
    }

    @Test
    fun theChoiceOfOneVehicleDoesNotAffectAnotherVehiclesForm() = runTest {
        seedRemembered("a", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("b", OdometerUnit.KILOMETERS, remembered = null)
        val a = LogEventProcessor("a", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures)
        a.dispatch(LogEventIntent.TenthsChanged(included = true))
        a.type(4)
        a.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(OdometerUnit.KILOMETERS_TENTHS, LogEventProcessor("a", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
        assertEquals(OdometerUnit.KILOMETERS, LogEventProcessor("b", repository, pictures, eventPictures, codec, clock, deviceZone, recognizer, captures).state.unit)
    }

    // ---- Scanning a reading (odometer-ocr-capture)

    private fun element(text: String, l: Int, t: Int, r: Int, b: Int) =
        com.mikonoma.drivinglog.vehicle.ocr.RecognizedElement(text, com.mikonoma.drivinglog.vehicle.ocr.TextBox(l, t, r, b))

    private fun line(vararg elements: com.mikonoma.drivinglog.vehicle.ocr.RecognizedElement) = com.mikonoma.drivinglog.vehicle.ocr.RecognizedLine(
        elements.joinToString(" ") { it.text },
        com.mikonoma.drivinglog.vehicle.ocr.TextBox(elements.minOf { it.box.left }, elements.minOf { it.box.top }, elements.maxOf { it.box.right }, elements.maxOf { it.box.bottom }),
        elements.toList(),
    )

    /** A dashboard with an odometer reading (45 260, just above the known 45 230) and a trip reading of 123.4. */
    private val dashboard = com.mikonoma.drivinglog.vehicle.ocr.RecognizedPhoto(
        1280, 720,
        listOf(
            line(element("ODO", 524, 403, 554, 415)),
            line(element("45260km", 529, 412, 619, 433)),
            line(element("TRIP", 100, 600, 150, 620), element("123.4", 160, 600, 230, 625), element("km", 235, 605, 260, 625)),
            line(element("RPMx", 340, 652, 361, 665), element("1000", 363, 654, 381, 667)),
        ),
    )

    private fun LogEventProcessor.scan() {
        recognizer.photo = dashboard
        dispatch(LogEventIntent.ScanPhotoPicked(com.mikonoma.drivinglog.vehicle.picture.PhotoResult.Chosen(byteArrayOf(1, 2, 3))))
    }

    private fun LogEventProcessor.indexOf(value: String) = state.scan.review!!.detections.indexOfFirst { it.value == value }

    private fun LogEventProcessor.accept(value: String) {
        dispatch(LogEventIntent.ScanCandidateSelected(indexOf(value)))
        dispatch(LogEventIntent.ScanConfirmed)
    }

    @Test
    fun scanningOpensTheReviewWithTheCandidatesClassifiedAgainstTheKnownOdometer() {
        seedVehicle()
        val processor = processor()

        processor.scan()

        val review = assertNotNull(processor.state.scan.review)
        val kinds = review.detections.associate { it.value to it.kind }
        assertEquals(com.mikonoma.drivinglog.vehicle.ocr.ReadingKind.ODOMETER, kinds["45260"])
        assertEquals(com.mikonoma.drivinglog.vehicle.ocr.ReadingKind.TRIP, kinds["123.4"])
        assertNull(kinds["1000"])
        assertNull(review.selectedIndex)
        assertEquals(1, captures.pending.size)
        assertNotNull(processor.state.scanPhotoUri)
        assertFalse(processor.state.isScanning)
    }

    @Test
    fun thePhotoIsKeptOnceFullSizeAndUncropped() {
        seedVehicle()
        val processor = processor()

        processor.scan()

        assertEquals(listOf(com.mikonoma.drivinglog.vehicle.picture.MAX_DECODE_SIDE), codec.scaledEncodes.single().caps)
    }

    @Test
    fun acceptingAnOdometerReadingOnTripDistanceSwitchesTheWayAndSetsTheField() {
        seedVehicle()
        val processor = processor()
        processor.type(3, 0)

        processor.scan()
        processor.accept("45260")

        assertEquals(LogWay.NEW_ODOMETER, processor.state.way)
        assertEquals(45_260L, processor.state.newOdometer.steps)
        assertEquals(30L, processor.state.tripDistance.steps) // the other way keeps what was typed
        assertNull(processor.state.scan.review)
        assertEquals("45260", processor.state.scan.accepted?.result?.accepted?.value)
    }

    @Test
    fun acceptingATripReadingOnNewOdometerSwitchesTheWayAndTurnsTenthsOn() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))

        processor.scan()
        processor.accept("123.4")

        assertEquals(LogWay.TRIP_DISTANCE, processor.state.way)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, processor.state.unit)
        assertEquals(1_234L, processor.state.tripDistance.steps)
    }

    @Test
    fun aWholeReadingInAUnitWithTenthsGetsAZeroTenth() {
        seedVehicle(unit = OdometerUnit.KILOMETERS_TENTHS)
        val processor = processor()

        processor.scan()
        processor.accept("45260")

        assertEquals(OdometerUnit.KILOMETERS_TENTHS, processor.state.unit)
        assertEquals(452_600L, processor.state.newOdometer.steps)
    }

    @Test
    fun selectingAnotherCandidateReplacesTheSelectionAndANonCandidateIsIgnored() {
        seedVehicle()
        val processor = processor()
        processor.scan()

        processor.dispatch(LogEventIntent.ScanCandidateSelected(processor.indexOf("45260")))
        processor.dispatch(LogEventIntent.ScanCandidateSelected(processor.indexOf("123.4")))
        processor.dispatch(LogEventIntent.ScanCandidateSelected(processor.indexOf("1000")))

        assertEquals("123.4", processor.state.scan.review?.selected?.value)
    }

    @Test
    fun confirmingWithNothingSelectedChangesNothing() {
        seedVehicle()
        val processor = processor()
        processor.scan()

        processor.dispatch(LogEventIntent.ScanConfirmed)

        assertNotNull(processor.state.scan.review)
        assertNull(processor.state.scan.accepted)
    }

    @Test
    fun backingOutOfTheReviewLeavesTheFormAsItWasAndDropsThePhoto() {
        seedVehicle()
        val processor = processor()
        processor.type(3, 0)
        processor.scan()
        processor.dispatch(LogEventIntent.ScanCandidateSelected(processor.indexOf("45260")))

        processor.dispatch(LogEventIntent.ScanCancelled)

        assertEquals(LogWay.TRIP_DISTANCE, processor.state.way)
        assertEquals(30L, processor.state.tripDistance.steps)
        assertNull(processor.state.scan.review)
        assertNull(processor.state.scan.accepted)
        assertTrue(captures.pending.isEmpty())
    }

    @Test
    fun cancellingTheChooserChangesNothing() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.ScanPhotoPicked(com.mikonoma.drivinglog.vehicle.picture.PhotoResult.Cancelled))

        assertEquals(com.mikonoma.drivinglog.vehicle.ocr.ScanDraft(), processor.state.scan)
        assertTrue(recognizer.recognized.isEmpty())
    }

    @Test
    fun anUnreadablePhotoSaysSoAndOpensNoReview() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.ScanPhotoPicked(com.mikonoma.drivinglog.vehicle.picture.PhotoResult.Chosen(byteArrayOf())))

        assertEquals(com.mikonoma.drivinglog.vehicle.picture.PictureError.COULD_NOT_OPEN, processor.state.scan.error)
        assertNull(processor.state.scan.review)
        processor.dispatch(LogEventIntent.ScanErrorDismissed)
        assertNull(processor.state.scan.error)
    }

    @Test
    fun aPhotoWithNoReadingOpensTheReviewWithNoCandidates() {
        seedVehicle()
        val processor = processor()
        recognizer.photo = com.mikonoma.drivinglog.vehicle.ocr.RecognizedPhoto(983, 1310, listOf(line(element("RPMx", 340, 652, 361, 665), element("1000", 363, 654, 381, 667))))

        processor.dispatch(LogEventIntent.ScanPhotoPicked(com.mikonoma.drivinglog.vehicle.picture.PhotoResult.Chosen(byteArrayOf(1))))

        assertFalse(assertNotNull(processor.state.scan.review).hasCandidates)
    }

    @Test
    fun choosingAnotherPhotoFromTheReviewReplacesIt() {
        seedVehicle()
        val processor = processor()
        processor.scan()
        val first = processor.state.scan.review!!.pendingId

        processor.scan()

        assertNotEquals(first, processor.state.scan.review!!.pendingId)
        assertEquals(listOf(first), captures.discarded)
    }

    @Test
    fun savingAfterAcceptingSavesTheScanWithTheEntry() = runTest {
        seedVehicle()
        val processor = processor()
        processor.scan()
        processor.accept("123.4")

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val capture = assertNotNull(repository.distanceCalls.single().capture)
        assertEquals("123.4", capture.result.accepted.value)
        assertEquals(3, capture.result.detections.size) // every number found, not only the accepted one
    }

    @Test
    fun savingAnOdometerAnchorAfterAcceptingSavesTheScanWithIt() = runTest {
        repository.seedVehicle("v1", "Family car")
        repository.seedEvents("v1", emptyList())
        val processor = processor()
        processor.scan()
        processor.accept("45260")

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertNotNull(repository.anchorCalls.single().capture)
    }

    @Test
    fun aSecondAcceptedScanReplacesTheFirst() = runTest {
        seedVehicle()
        val processor = processor()
        processor.scan()
        processor.accept("45260")
        val first = processor.state.scan.accepted!!.pendingId
        processor.scan()
        processor.accept("123.4")

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertTrue(first in captures.discarded)
        assertEquals("123.4", repository.distanceCalls.single().capture?.result?.accepted?.value)
    }

    @Test
    fun aTypedEntrySavesNoScan() = runTest {
        seedVehicle()
        val processor = processor()
        processor.type(1, 2)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertNull(repository.distanceCalls.single().capture)
    }

    @Test
    fun leavingAfterAcceptingDiscardsTheScan() {
        seedVehicle()
        val processor = processor()
        processor.scan()
        processor.accept("45260")
        val pendingId = processor.state.scan.accepted!!.pendingId

        processor.dispatch(LogEventIntent.Left)

        assertTrue(pendingId in captures.discarded)
        assertTrue(captures.pending.isEmpty())
        assertTrue(captures.photos.isEmpty())
        assertEquals(emptyList(), repository.distanceCalls)
    }

    @Test
    fun theScanActionIsOfferedOnlyWhereThereIsARecognizer() {
        seedVehicle()
        assertTrue(processor().state.canScan)

        val without = LogEventProcessor(
            "v1", repository, pictures, eventPictures, codec, clock, deviceZone,
            com.mikonoma.drivinglog.vehicle.ocr.UnavailableTextRecognizer, captures,
        )

        assertFalse(without.state.canScan)
    }
    // ---- The live scanner (add-live-scanner)

    private fun liveReading(value: String, kind: com.mikonoma.drivinglog.vehicle.ocr.ReadingKind): com.mikonoma.drivinglog.vehicle.ocr.LiveReading {
        val detections = listOf(
            com.mikonoma.drivinglog.vehicle.ocr.Detection("RPMx", "1000", com.mikonoma.drivinglog.vehicle.ocr.TextBox(0, 0, 10, 10), null, com.mikonoma.drivinglog.vehicle.ocr.DetectionBasis.NO_UNIT),
            com.mikonoma.drivinglog.vehicle.ocr.Detection(value, value, com.mikonoma.drivinglog.vehicle.ocr.TextBox(500, 400, 600, 430), kind, com.mikonoma.drivinglog.vehicle.ocr.DetectionBasis.LABEL, "ODO"),
        )
        val frame = com.mikonoma.drivinglog.vehicle.ocr.LiveFrame(com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage(1280, 720, IntArray(1280 * 720)), detections)
        return com.mikonoma.drivinglog.vehicle.ocr.LiveReading(1, detections[1], frame, now)
    }

    @Test
    fun openingAndClosingTheScannerLeavesTheFormAsItWasAndKeepsNothing() {
        seedVehicle()
        val processor = processor()
        processor.type(3, 0)

        processor.dispatch(LogEventIntent.ScannerOpened)
        assertTrue(processor.state.scan.scannerOpen)
        processor.dispatch(LogEventIntent.ScannerClosed)

        assertFalse(processor.state.scan.scannerOpen)
        assertEquals(LogWay.TRIP_DISTANCE, processor.state.way)
        assertEquals(30L, processor.state.tripDistance.steps)
        assertNull(processor.state.scan.accepted)
        assertTrue(captures.pending.isEmpty())
    }

    @Test
    fun tappingALiveOdometerReadingAppliesItKeepsItsFrameAndClosesTheScanner() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.ScannerOpened)
        val reading = liveReading("45260", com.mikonoma.drivinglog.vehicle.ocr.ReadingKind.ODOMETER)

        processor.dispatch(LogEventIntent.LiveReadingTapped(reading))

        assertFalse(processor.state.scan.scannerOpen)
        assertEquals(LogWay.NEW_ODOMETER, processor.state.way)
        assertEquals(45_260L, processor.state.newOdometer.steps)
        val accepted = assertNotNull(processor.state.scan.accepted)
        assertEquals("45260", accepted.result.accepted.value)
        assertEquals(2, accepted.result.detections.size) // the frame's every detection, not only the tapped one
        assertEquals(1280 to 720, accepted.result.width to accepted.result.height)
        assertEquals(listOf(reading.frame.image), codec.encodedFrames)
        assertEquals(1, captures.pending.size)
    }

    @Test
    fun tappingALiveTripReadingOnNewOdometerSwitchesTheWay() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.dispatch(LogEventIntent.ScannerOpened)

        processor.dispatch(LogEventIntent.LiveReadingTapped(liveReading("168.1", com.mikonoma.drivinglog.vehicle.ocr.ReadingKind.TRIP)))

        assertEquals(LogWay.TRIP_DISTANCE, processor.state.way)
        assertEquals(1_681L, processor.state.tripDistance.steps)
    }

    @Test
    fun aLiveReadingReplacesAnEarlierAcceptedScan() = runTest {
        seedVehicle()
        val processor = processor()
        processor.scan()
        processor.accept("45260")
        val first = processor.state.scan.accepted!!.pendingId
        processor.dispatch(LogEventIntent.ScannerOpened)
        processor.dispatch(LogEventIntent.LiveReadingTapped(liveReading("45270", com.mikonoma.drivinglog.vehicle.ocr.ReadingKind.ODOMETER)))

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertTrue(first in captures.discarded)
        assertEquals("45270", repository.anchorCalls.singleOrNull()?.capture?.result?.accepted?.value ?: repository.distanceCalls.single().capture?.result?.accepted?.value)
    }

    @Test
    fun leavingThePhotoReviewReturnsToTheScanner() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.ScannerOpened)
        processor.scan()
        assertNotNull(processor.state.scan.review)

        processor.dispatch(LogEventIntent.ScanCancelled)

        assertNull(processor.state.scan.review)
        assertTrue(processor.state.scan.scannerOpen)
    }

    @Test
    fun confirmingOnThePhotoReviewClosesTheScannerToo() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.ScannerOpened)
        processor.scan()

        processor.accept("45260")

        assertNull(processor.state.scan.review)
        assertFalse(processor.state.scan.scannerOpen)
        assertEquals(LogWay.NEW_ODOMETER, processor.state.way)
    }

    @Test
    fun anOpenScannerSurvivesSavedState() {
        val json = kotlinx.serialization.json.Json
        val draft = com.mikonoma.drivinglog.vehicle.ocr.ScanDraft(scannerOpen = true)

        val restored = json.decodeFromString(com.mikonoma.drivinglog.vehicle.ocr.ScanDraft.serializer(), json.encodeToString(com.mikonoma.drivinglog.vehicle.ocr.ScanDraft.serializer(), draft))

        assertTrue(restored.scannerOpen)
    }

    // ---- Refueling (add-refueling-logging)

    private fun LogEventProcessor.typeFuelAmount(vararg digits: Int) {
        for (d in digits) dispatch(LogEventIntent.FuelAmountEdited(state.fuelAmount.digits + d))
    }

    @Test
    fun theRefuelingFormStartsWithFilledUpCheckedAndAnEmptyFuelAmount() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))

        assertTrue(processor.state.filledUp)
        assertTrue(processor.state.fuelAmount.isEmpty)
        assertFalse(processor.state.hasRequiredField)
    }

    @Test
    fun saveStaysDisabledWithAnEmptyFuelAmountRegardlessOfTheMileageSection() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))

        assertFalse(processor.state.hasRequiredField)

        processor.type(3, 0)
        assertFalse(processor.state.hasRequiredField)
    }

    @Test
    fun saveEnablesOnceAFuelAmountIsTypedWithNoMileage() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))

        processor.typeFuelAmount(4, 2, 3)

        assertTrue(processor.state.hasRequiredField)
    }

    @Test
    fun savingWithAZeroFuelAmountShowsAnErrorAndSavesNothing() = runTest {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(0)

        processor.dispatch(LogEventIntent.Save)

        assertEquals(LogDistanceError.FuelAmountNotPositive, processor.error())
        assertTrue(repository.refuelingCalls.isEmpty())
    }

    @Test
    fun aFutureMomentIsRefusedForARefuelingToo() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(4, 2, 3)
        processor.dispatch(LogEventIntent.DateChanged(LocalDate(2026, 9, 21)))

        processor.dispatch(LogEventIntent.Save)

        assertEquals(LogDistanceError.TimeInFuture, processor.error())
        assertTrue(repository.refuelingCalls.isEmpty())
    }

    @Test
    fun savingWithNoMileageReachesTheRepositoryWithNoMileage() = runTest {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(4, 2, 3)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val call = repository.refuelingCalls.single()
        assertNull(call.mileage)
        assertEquals(4_230L, call.amount.milliliters)
    }

    @Test
    fun savingWithTripDistanceMileageIncludesIt() = runTest {
        seedVehicle() // known/current odometer 45,230 km (45,200 initial + a seeded 30 km entry)
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(4, 2, 3)
        processor.type(3, 0)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(RefuelingMileage.Added(Distance(30_000)), repository.refuelingCalls.single().mileage)
    }

    @Test
    fun savingWithNewOdometerMileageIncludesTheLoggedCount() = runTest {
        seedVehicle() // known/current odometer 45,230 km
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(4, 2, 3)
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 5, 2, 5, 0)

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val mileage = repository.refuelingCalls.single().mileage as? RefuelingMileage.Added
        assertNotNull(mileage)
        assertEquals(Distance(20_000), mileage.distance)
        assertEquals(Distance(45_250_000), mileage.loggedOdometer)
    }

    @Test
    fun aLowerNewOdometerMileageNeedsConfirmationAndSavesAsAnAnchor() = runTest {
        seedVehicle() // known/current odometer 45,230 km
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(4, 2, 3)
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
        processor.type(4, 4, 0, 0, 0)
        processor.dispatch(LogEventIntent.Save)

        assertTrue(processor.state.lowerOdometerConfirmationPending)
        assertTrue(repository.refuelingCalls.isEmpty())

        processor.test {
            dispatch(LogEventIntent.LowerOdometerConfirmed)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(RefuelingMileage.Anchor(Distance(44_000_000)), repository.refuelingCalls.single().mileage)
    }

    @Test
    fun savingHandsTheFilledUpStateAndFuelTypeToTheRepository() = runTest {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(4, 2, 3)
        processor.dispatch(LogEventIntent.FuelTypeSelected(FuelType.DIESEL))
        processor.dispatch(LogEventIntent.FilledUpChanged(false))

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val call = repository.refuelingCalls.single()
        assertEquals(FuelType.DIESEL, call.fuelType)
        assertFalse(call.filledUp)
    }

    @Test
    fun savingARefuelingWithANoteAndAPhotoReachesTheRepository() = runTest {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(4, 2, 3)
        processor.dispatch(LogEventIntent.NoteEditorOpened)
        processor.dispatch(LogEventIntent.NoteDraftEdited("cheap gas today"))
        processor.dispatch(LogEventIntent.NoteAttached)
        processor.pick()

        processor.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        val call = repository.refuelingCalls.single()
        assertEquals("cheap gas today", call.note)
        assertEquals(1, call.photos.size)
    }

    // ---- The fuel type filter (vehicle-fuel-type)

    @Test
    fun allowedFuelTypesMatchesTheVehiclesFuelType() {
        seedVehicle(fuelType = VehicleFuelType.DIESEL)

        assertEquals(
            setOf(FuelType.DIESEL, FuelType.PREMIUM_DIESEL, FuelType.BIODIESEL, FuelType.OTHER),
            processor().state.allowedFuelTypes,
        )
    }

    @Test
    fun noRememberedPreferenceDefaultsToTheVehiclesFirstOfferedType() {
        seedVehicle(fuelType = VehicleFuelType.DIESEL)

        assertEquals(FuelType.DIESEL, processor().state.fuelType)
    }

    @Test
    fun theRememberedChoiceFallsBackWhenTheVehicleDoesNotOfferIt() = runTest {
        repository.seedLastFuelType(FuelType.LPG)
        seedVehicle(fuelType = VehicleFuelType.DIESEL)

        val state = processor().state

        assertEquals(FuelType.DIESEL, state.fuelType) // Diesel's first offered type, not the remembered LPG
        assertEquals(FuelType.LPG, repository.observeLastFuelType().first()) // the remembered choice itself is untouched
    }

    @Test
    fun switchingToAVehicleWithADifferentFuelTypeReEvaluatesTheFilter() {
        repository.seedVehicle("petrol-car", "petrol-car", fuelType = VehicleFuelType.PETROL)
        repository.seedEvents("petrol-car", listOf(initialEvent("i-petrol", initialAt, 45_200_000)))
        repository.seedVehicle("diesel-car", "diesel-car", fuelType = VehicleFuelType.DIESEL)
        repository.seedEvents("diesel-car", listOf(initialEvent("i-diesel", initialAt, 45_200_000)))
        val processor = chooser()
        assertEquals("diesel-car", processor.state.selectedVehicleId) // alphabetically first

        processor.dispatch(LogEventIntent.VehicleSelected("petrol-car"))

        assertEquals(FuelType.REGULAR_PETROL, processor.state.fuelType)
        assertEquals(setOf(FuelType.REGULAR_PETROL, FuelType.PREMIUM_PETROL, FuelType.E85, FuelType.OTHER), processor.state.allowedFuelTypes)
    }

    @Test
    fun aManuallyChosenFuelTypeThatIsStillOfferedSurvivesAVehicleSwitch() {
        repository.seedVehicle("aaa-petrol", "aaa-petrol", fuelType = VehicleFuelType.PETROL)
        repository.seedEvents("aaa-petrol", listOf(initialEvent("i-petrol", initialAt, 45_200_000)))
        repository.seedVehicle("zzz-any", "zzz-any", fuelType = VehicleFuelType.OTHER)
        repository.seedEvents("zzz-any", listOf(initialEvent("i-any", initialAt, 45_200_000)))
        val processor = chooser()
        assertEquals("aaa-petrol", processor.state.selectedVehicleId)
        processor.dispatch(LogEventIntent.FuelTypeSelected(FuelType.E85)) // offered by Petrol

        processor.dispatch(LogEventIntent.VehicleSelected("zzz-any")) // offers every fuel type, including E85

        assertEquals(FuelType.E85, processor.state.fuelType)
    }

    @Test
    fun aManuallyChosenFuelTypeThatIsNoLongerOfferedFallsBackOnAVehicleSwitch() {
        repository.seedVehicle("aaa-any", "aaa-any", fuelType = VehicleFuelType.OTHER)
        repository.seedEvents("aaa-any", listOf(initialEvent("i-any", initialAt, 45_200_000)))
        repository.seedVehicle("zzz-lpg", "zzz-lpg", fuelType = VehicleFuelType.LPG)
        repository.seedEvents("zzz-lpg", listOf(initialEvent("i-lpg", initialAt, 45_200_000)))
        val processor = chooser()
        assertEquals("aaa-any", processor.state.selectedVehicleId)
        processor.dispatch(LogEventIntent.FuelTypeSelected(FuelType.DIESEL)) // offered by the "Other" vehicle

        processor.dispatch(LogEventIntent.VehicleSelected("zzz-lpg")) // does not offer Diesel

        assertEquals(FuelType.LPG, processor.state.fuelType) // LPG's only (non-Other) offered type
    }

    @Test
    fun theFuelUnitAndTypeDefaultToTheRememberedPreference() {
        repository.seedLastFuelUnit(FuelUnit.GALLONS)
        repository.seedLastFuelType(FuelType.DIESEL)
        seedVehicle(fuelType = VehicleFuelType.DIESEL) // offers Diesel, so the remembered choice is honored

        val state = processor().state

        assertEquals(FuelUnit.GALLONS, state.fuelUnit)
        assertEquals(FuelType.DIESEL, state.fuelType)
    }

    @Test
    fun noRememberedPreferenceKeepsTheBuiltInDefaults() {
        seedVehicle()

        val state = processor().state

        assertEquals(FuelUnit.LITERS, state.fuelUnit)
        assertEquals(FuelType.REGULAR_PETROL, state.fuelType)
    }

    @Test
    fun aChosenFuelUnitIsNeverOverriddenOnceLoaded() {
        seedVehicle()
        val processor = processor()

        processor.dispatch(LogEventIntent.FuelUnitSelected(FuelUnit.GALLONS))
        // The global preference changing again afterward (as if another session saved a refueling meanwhile)
        // must not clobber this session's own choice.
        repository.seedLastFuelUnit(FuelUnit.LITERS)

        assertEquals(FuelUnit.GALLONS, processor.state.fuelUnit)
    }

    @Test
    fun changingTheFuelUnitKeepsTheDigitsTyped() {
        seedVehicle()
        val processor = processor()
        processor.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
        processor.typeFuelAmount(4, 2, 3)

        processor.dispatch(LogEventIntent.FuelUnitSelected(FuelUnit.GALLONS))

        assertEquals("423", processor.state.fuelAmount.digits)
        assertEquals(FuelUnit.GALLONS, processor.state.fuelUnit)
    }

    @Test
    fun theRefuelingFieldsSurviveRestoreState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        seedVehicle()
        val saved = processor().let { first ->
            first.dispatch(LogEventIntent.KindSelected(LogKind.REFUELING))
            first.dispatch(LogEventIntent.FuelAmountEdited("423"))
            first.dispatch(LogEventIntent.FuelUnitSelected(FuelUnit.GALLONS))
            first.dispatch(LogEventIntent.FuelTypeSelected(FuelType.DIESEL))
            first.dispatch(LogEventIntent.FilledUpChanged(false))
            advanceUntilIdle()
            checkNotNull(first.stateToSave())
        }

        val restored = processor()
        restored.restoreState(saved)
        advanceUntilIdle()

        assertEquals(LogKind.REFUELING, restored.state.kind)
        assertEquals("423", restored.state.fuelAmount.digits)
        assertEquals(FuelUnit.GALLONS, restored.state.fuelUnit)
        assertEquals(FuelType.DIESEL, restored.state.fuelType)
        assertFalse(restored.state.filledUp)
    }
}
