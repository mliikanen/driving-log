package com.mikonoma.drivinglog.vehicle.format

import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.locale.TimeFormat
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class EventRowContentTest {

    private val us = NumberSymbols.ENGLISH_US
    private val helsinki = TimeZone.of("Europe/Helsinki")
    private val newYork = TimeZone.of("America/New_York")
    private val at = ZonedMoment.of(Instant.parse("2026-09-20T12:30:00Z"), helsinki) // 15:30 in Helsinki

    private fun distance(meters: Long, logged: Long? = null, moment: ZonedMoment = at) =
        VehicleEvent.DistanceEntry("d", moment, Distance(meters), logged?.let { Distance(it) })

    private fun row(event: VehicleEvent, unit: OdometerUnit = OdometerUnit.KILOMETERS, zone: TimeZone = helsinki) =
        eventRowContent(event, unit, us, zone)

    @Test
    fun anInitialOdometerShowsItsReadingInTheVehiclesUnit() {
        val row = row(VehicleEvent.InitialOdometer("i", at, Distance(45_200_300)), OdometerUnit.KILOMETERS_TENTHS)

        assertEquals("Initial odometer", row.label)
        assertEquals("2026-09-20 15:30", row.moment)
        assertEquals("45,200.3 km", row.trailing)
        assertNull(row.loggedOdometer)
    }

    @Test
    fun aDistanceEntryShowsItsDistanceWithAPlusSign() {
        val row = row(distance(30_000))

        assertEquals("Distance", row.label)
        assertEquals("+30 km", row.trailing)
        assertNull(row.loggedOdometer)
    }

    @Test
    fun distancesInTenthsAreShownWithOneDecimal() {
        assertEquals("+12.3 km", row(distance(12_300), OdometerUnit.KILOMETERS_TENTHS).trailing)
        assertEquals("+4.5 km", row(distance(4_500), OdometerUnit.KILOMETERS_TENTHS).trailing)
    }

    @Test
    fun aDistanceIsShownInTheVehiclesUnitNotTheUnitItWasTypedIn() {
        // 10 miles is 16 093 m.
        assertEquals("+16.1 km", row(distance(16_093), OdometerUnit.KILOMETERS_TENTHS).trailing)
        assertEquals("+10 mi", row(distance(16_093), OdometerUnit.MILES).trailing)
    }

    @Test
    fun anEntryLoggedByOdometerShowsTheTypedCount() {
        val row = row(distance(50_000, logged = 45_250_000))

        assertEquals("+50 km", row.trailing)
        assertEquals("Odometer 45,250 km", row.loggedOdometer)
    }

    @Test
    fun theMomentIsShownInTheZoneItWasEnteredIn() {
        val entered = ZonedMoment.of(Instant.parse("2026-09-20T12:30:00Z"), newYork) // 08:30 in New York

        assertEquals("2026-09-20 08:30 (America/New_York)", row(distance(1_000, moment = entered), zone = helsinki).moment)
        assertEquals("2026-09-20 15:30", row(distance(1_000), zone = helsinki).moment)
    }

    @Test
    fun aRowKeepsItsZoneWhenTheDeviceZoneChanges() {
        val entered = ZonedMoment.of(Instant.parse("2026-09-20T15:30:00Z"), helsinki) // 18:30 in Helsinki

        assertEquals("2026-09-20 18:30 (Europe/Helsinki)", row(distance(1_000, moment = entered), zone = newYork).moment)
    }

    @Test
    fun aTwelveHourSettingIsAppliedToTheRowsTime() {
        val row = eventRowContent(distance(1_000), OdometerUnit.KILOMETERS, us, helsinki, TimeFormat(is24Hour = false))
        assertEquals("2026-09-20 3:30 PM", row.moment)
    }
}
