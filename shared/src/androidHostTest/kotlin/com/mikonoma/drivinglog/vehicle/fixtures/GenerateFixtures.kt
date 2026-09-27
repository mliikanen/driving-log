package com.mikonoma.drivinglog.vehicle.fixtures

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.vehicle.FixedDeviceTimeZone
import com.mikonoma.drivinglog.vehicle.data.SqlDelightVehicleRepository
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import com.mikonoma.drivinglog.vehicle.picture.FakePictureStore
import java.io.File
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone

/**
 * Regenerates the `.db` fixture files Maestro flows seed onto a device before a rendering-focused flow runs
 * (speed-up-tests-with-db-fixtures). Run on demand via `./gradlew :shared:generateMaestroFixtures`, never as part
 * of the ordinary test suite (it is excluded from `testAndroidHostTest`'s normal run by name, since it writes
 * files as a side effect rather than asserting anything) — its output is checked into `maestro/assets/fixtures/`
 * as a versioned asset, not regenerated at build or test time.
 *
 * Every fixture is built by calling [SqlDelightVehicleRepository]'s real methods — the exact class the shipped app
 * uses — against a real, file-backed SQLite database, the same way `SqlDelightVehicleRepositoryTest` already does
 * against an in-memory one. This is deliberate: a fixture's rows are only ever produced by calls the production
 * code itself validates, never hand-written SQL that could drift from the schema.
 *
 * A fixed clock (not wall-clock "now") is used throughout, so a fixture's dates and times are the same on every
 * regeneration and a Maestro flow can assert on them literally, with no relative-date trick needed.
 */
private class FixedClock(var current: Instant) : Clock {
    override fun now(): Instant = current
}

/** Must match the checked-in `fixture-vehicle-picture-{small,large}.png` files' names (see task 1.2). */
private const val FIXTURE_PICTURE_ID = "fixture-vehicle-picture"

class GenerateFixturesTest {
    @Test
    fun generate() {
        val targetDir = File(
            System.getProperty("maestroFixturesDir")
                ?: error("system property maestroFixturesDir not set (run via :shared:generateMaestroFixtures)"),
        )
        targetDir.mkdirs()
        generateVehicleWithLogAndNote(File(targetDir, "vehicle-with-log-and-note.db"))
    }
}

private fun generateVehicleWithLogAndNote(target: File) {
    target.delete()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${target.absolutePath}")
    DrivingLogDatabase.Schema.create(driver)
    // AndroidSqliteDriver (the production driver, unlike this plain JDBC one) decides whether to run its own
    // create/migrate callback by reading PRAGMA user_version — plain Schema.create() over JDBC never sets it, so
    // without this the app re-runs schema creation on top of the already-seeded data on first open, which breaks
    // its reactive queries entirely (confirmed on-device: every "SQLiteLog: table ... already exists" error, and
    // the home screen's vehicle action stuck disabled forever, though the underlying data was intact).
    driver.execute(null, "PRAGMA user_version = ${DrivingLogDatabase.Schema.version}", 0)
    val database = DrivingLogDatabase(driver)
    val clock = FixedClock(Instant.parse("2026-01-15T12:00:00Z"))
    var counter = 0
    val repository = SqlDelightVehicleRepository(
        database = database,
        clock = clock,
        newId = { "fixture-${++counter}" },
        dispatcher = Dispatchers.Unconfined,
        deviceTimeZone = FixedDeviceTimeZone(TimeZone.UTC),
        pictures = FakePictureStore(),
    )

    runBlocking {
        val vehicleId = repository.addVehicle(
            name = "Fixture Car",
            licensePlate = "FIX-001",
            type = VehicleType.CAR,
            color = VehicleColors.default,
            unit = OdometerUnit.KILOMETERS_TENTHS,
            initialOdometer = Distance(45_200_000),
        )
        clock.current = Instant.parse("2026-01-16T09:00:00Z")
        repository.addDistanceEntry(
            vehicleId = vehicleId,
            occurredAt = ZonedMoment(clock.current, null),
            distance = Distance(30_000),
            loggedOdometer = null,
            tenthsIncluded = true,
        )
        clock.current = Instant.parse("2026-01-17T09:00:00Z")
        repository.addDistanceEntry(
            vehicleId = vehicleId,
            occurredAt = ZonedMoment(clock.current, null),
            distance = Distance(10_000),
            loggedOdometer = null,
            tenthsIncluded = true,
            note = "borrowed to Sam",
        )

        // The picture itself is a static asset (maestro/assets/fixtures/fixture-vehicle-picture-{small,large}.png
        // — no JVM ImageCodec exists to produce one here, see design.md), but wiring its id onto the vehicle row
        // uses the real, generated query the repository itself calls internally for a picture change — not a
        // hand-written UPDATE — so this one column is still never touched by ad-hoc SQL.
        database.vehicleQueries.updateVehiclePicture(FIXTURE_PICTURE_ID, clock.current.toEpochMilliseconds(), vehicleId)
    }

    driver.close()
    println("Wrote ${target.absolutePath}")
}
