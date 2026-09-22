package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import com.mikonoma.drivinglog.vehicle.FixedDeviceTimeZone
import com.mikonoma.drivinglog.vehicle.anchorEvent
import com.mikonoma.drivinglog.vehicle.data.FakeClock
import com.mikonoma.drivinglog.vehicle.distanceEvent
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.initialEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private fun seedVehicle(unit: OdometerUnit = OdometerUnit.KILOMETERS, withEntry: Boolean = true) {
        repository.seedVehicle("v1", "Family car", unit = unit)
        val entries = if (withEntry) listOf(distanceEvent("d1", initialAt + 1.hours.inWholeMilliseconds, 30_000)) else emptyList()
        repository.seedEvents("v1", entries + initialEvent("i1", initialAt, 45_200_000))
    }

    private val pictures = com.mikonoma.drivinglog.vehicle.picture.FakeVehiclePictureStore()

    private fun processor(vehicleId: String = "v1") = LogEventProcessor(vehicleId, repository, pictures, clock, deviceZone)

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

            val state = LogEventProcessor(unit.name, repository, pictures, clock, deviceZone).state

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
    fun aLowerOrEqualCountIsRefusedNamingTheKnownOdometer() {
        seedVehicle() // known odometer now: 45 200 km + 30 km
        val processor = processor()
        processor.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))

        processor.type(4, 5, 1, 0, 0)
        processor.dispatch(LogEventIntent.Save)
        assertEquals(LogDistanceError.OdometerNotHigher(Distance(45_230_000)), processor.error())

        processor.dispatch(LogEventIntent.OdometerCleared)
        processor.type(4, 5, 2, 3, 0)
        processor.dispatch(LogEventIntent.Save)
        assertEquals(LogDistanceError.OdometerNotHigher(Distance(45_230_000)), processor.error())
        assertEquals(emptyList(), repository.distanceCalls)
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
        val processor = LogEventProcessor("missing", repository, pictures, clock, deviceZone)

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
        val saved = LogEventProcessor("v1", repository, pictures, clock, deviceZone).let { first ->
            advanceUntilIdle()
            first.dispatch(LogEventIntent.UnitFamilySelected(miles = true))
            first.dispatch(LogEventIntent.WayChanged(LogWay.NEW_ODOMETER))
            first.dispatch(LogEventIntent.ZoneChanged("Asia/Tokyo"))
            advanceUntilIdle()
            first.dispatch(LogEventIntent.OdometerEdited("123"))
            advanceUntilIdle()
            checkNotNull(first.stateToSave())
        }

        val restored = LogEventProcessor("v1", repository, pictures, clock, deviceZone)
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

    // ---- Choosing a vehicle (opened from the Home screen: an empty vehicle id)

    private fun chooser() = LogEventProcessor("", repository, pictures, clock, deviceZone)

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

        assertEquals(OdometerUnit.KILOMETERS_TENTHS, LogEventProcessor("a", repository, pictures, clock, deviceZone).state.unit)
        assertEquals(OdometerUnit.MILES, LogEventProcessor("b", repository, pictures, clock, deviceZone).state.unit)
    }

    @Test
    fun withNoRememberedChoiceTheTenthsOfTheVehiclesUnitAreUsed() {
        seedRemembered("a", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("b", OdometerUnit.KILOMETERS_TENTHS, remembered = null)

        assertEquals(OdometerUnit.KILOMETERS, LogEventProcessor("a", repository, pictures, clock, deviceZone).state.unit)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, LogEventProcessor("b", repository, pictures, clock, deviceZone).state.unit)
    }

    @Test
    fun onlyTheTenthsChoiceIsRememberedNotTheUnitFamily() {
        // A miles vehicle whose remembered choice is "without tenths" starts in miles, and a kilometer vehicle keeps kilometers.
        seedRemembered("m", OdometerUnit.MILES, remembered = true)
        seedRemembered("k", OdometerUnit.KILOMETERS, remembered = true)

        assertEquals(OdometerUnit.MILES_TENTHS, LogEventProcessor("m", repository, pictures, clock, deviceZone).state.unit)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, LogEventProcessor("k", repository, pictures, clock, deviceZone).state.unit)
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
        val left = LogEventProcessor("a", repository, pictures, clock, deviceZone)
        left.dispatch(LogEventIntent.TenthsChanged(included = true))
        left.type(1, 2)
        // The form is left without saving.

        val next = LogEventProcessor("a", repository, pictures, clock, deviceZone)

        assertEquals(OdometerUnit.KILOMETERS, next.state.unit)
        assertEquals(emptyList(), repository.distanceCalls)
    }

    @Test
    fun aRefusedSaveDoesNotRememberTheChoice() {
        seedRemembered("a", OdometerUnit.KILOMETERS, remembered = null)
        val processor = LogEventProcessor("a", repository, pictures, clock, deviceZone)
        processor.dispatch(LogEventIntent.TenthsChanged(included = true))

        processor.dispatch(LogEventIntent.Save) // nothing typed

        assertEquals(LogDistanceError.FieldEmpty, processor.state.error)
        assertEquals(OdometerUnit.KILOMETERS, LogEventProcessor("a", repository, pictures, clock, deviceZone).state.unit)
    }

    @Test
    fun theChoiceOfOneVehicleDoesNotAffectAnotherVehiclesForm() = runTest {
        seedRemembered("a", OdometerUnit.KILOMETERS, remembered = null)
        seedRemembered("b", OdometerUnit.KILOMETERS, remembered = null)
        val a = LogEventProcessor("a", repository, pictures, clock, deviceZone)
        a.dispatch(LogEventIntent.TenthsChanged(included = true))
        a.type(4)
        a.test {
            dispatch(LogEventIntent.Save)
            expectSideEffect(LogEventEffect.Saved)
        }

        assertEquals(OdometerUnit.KILOMETERS_TENTHS, LogEventProcessor("a", repository, pictures, clock, deviceZone).state.unit)
        assertEquals(OdometerUnit.KILOMETERS, LogEventProcessor("b", repository, pictures, clock, deviceZone).state.unit)
    }
}
