package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.vehicle.FixedDeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.EventZone
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone

/** The schema exactly as it was released in version 1, before distance entries and time zones. */
private val VERSION_1_SCHEMA = listOf(
    """
    CREATE TABLE vehicle (
        id            TEXT    NOT NULL PRIMARY KEY,
        name          TEXT    NOT NULL,
        license_plate TEXT,
        odometer_unit TEXT    NOT NULL,
        created_at    INTEGER NOT NULL,
        updated_at    INTEGER NOT NULL
    )
    """,
    """
    CREATE TABLE vehicle_event (
        id              TEXT    NOT NULL PRIMARY KEY,
        vehicle_id      TEXT    NOT NULL REFERENCES vehicle(id),
        type            TEXT    NOT NULL,
        occurred_at     INTEGER NOT NULL,
        odometer_meters INTEGER,
        created_at      INTEGER NOT NULL
    )
    """,
    "CREATE INDEX vehicle_event_by_vehicle ON vehicle_event (vehicle_id, occurred_at DESC)",
)

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleMigrationJvmTest {

    private val drivers = mutableListOf<SqlDriver>()

    @AfterTest
    fun close() = drivers.forEach { it.close() }

    private fun memoryDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { drivers += it }

    /** A database as an older build left it: the version-1 schema with one vehicle and its initial event. */
    private fun versionOneDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_1_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS_TENTHS', 1000, 1000)", 0)
        driver.execute(null, "INSERT INTO vehicle_event VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200300, 1785067200000)", 0)
        return driver
    }

    private fun repository(driver: SqlDriver) = SqlDelightVehicleRepository(
        database = DrivingLogDatabase(driver),
        clock = com.mikonoma.drivinglog.vehicle.data.FakeClock(Instant.parse("2026-09-20T12:00:00Z")),
        newId = generateSequence(1) { it + 1 }.map { "new-$it" }.iterator().let { ids -> { ids.next() } },
        dispatcher = UnconfinedTestDispatcher(),
        deviceTimeZone = FixedDeviceTimeZone(TimeZone.of("Europe/Helsinki")),
    )

    private fun columns(driver: SqlDriver, table: String): List<String> =
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($table)",
            mapper = { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names += cursor.getString(1)!!
                QueryResult.Value(names)
            },
            parameters = 0,
        ).value

    @Test
    fun migratingKeepsTheExistingVehicleAndEvent() = runTest {
        val driver = versionOneDatabase()

        DrivingLogDatabase.Schema.migrate(driver, 1, 2)

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        assertEquals("ABC-123", vehicle.licensePlate)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, vehicle.odometerUnit)
        val event = repository.observeLog("v1").first().single() as VehicleEvent.InitialOdometer
        assertEquals(Distance(45_200_300), event.reading)
        assertEquals(Instant.fromEpochMilliseconds(1_785_067_200_000), event.occurredAt.instant)
    }

    @Test
    fun aMigratedEventHasNoZoneAndItsNewColumnsAreNull() = runTest {
        val driver = versionOneDatabase()

        DrivingLogDatabase.Schema.migrate(driver, 1, 2)

        val event = repository(driver).observeLog("v1").first().single()
        assertNull(event.occurredAt.zone)
        val nulls = driver.executeQuery(
            identifier = null,
            sql = "SELECT distance_meters, logged_odometer_meters, occurred_zone, occurred_offset_seconds FROM vehicle_event",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value((0..3).map { cursor.getString(it) })
            },
            parameters = 0,
        ).value
        assertEquals(listOf(null, null, null, null), nulls)
    }

    @Test
    fun theCurrentOdometerOfAMigratedVehicleIsStillItsReading() = runTest {
        val driver = versionOneDatabase()
        DrivingLogDatabase.Schema.migrate(driver, 1, 2)

        assertEquals(Distance(45_200_300), repository(driver).observeVehicle("v1").first()?.currentOdometer)
    }

    @Test
    fun aDistanceEntryWithAZoneCanBeAddedAfterMigrating() = runTest {
        val driver = versionOneDatabase()
        DrivingLogDatabase.Schema.migrate(driver, 1, 2)
        val repository = repository(driver)
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T13:00:00Z"), TimeZone.of("America/New_York"))

        repository.addDistanceEntry("v1", moment, Distance(30_000), loggedOdometer = null)

        val entry = repository.observeLog("v1").first().first() as VehicleEvent.DistanceEntry
        assertEquals(EventZone("America/New_York", -4 * 3600), entry.occurredAt.zone)
        assertEquals(Distance(45_230_300), repository.observeVehicle("v1").first()?.currentOdometer)
    }

    @Test
    fun aMigratedVehicleCanAlsoGetANewVehicleWithTheDeviceZone() = runTest {
        val driver = versionOneDatabase()
        DrivingLogDatabase.Schema.migrate(driver, 1, 2)
        val repository = repository(driver)

        val id = repository.addVehicle("Van", null, OdometerUnit.MILES, Distance(1_609_344))

        assertEquals(EventZone("Europe/Helsinki", 3 * 3600), repository.observeLog(id).first().single().occurredAt.zone)
    }

    @Test
    fun aMigratedDatabaseHasTheSameTablesAsAFreshOne() {
        val migrated = versionOneDatabase().also { DrivingLogDatabase.Schema.migrate(it, 1, 2) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
    }

    @Test
    fun aFreshDatabaseIsVersionTwoWithTheNewColumns() {
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(2L, DrivingLogDatabase.Schema.version)
        assertEquals(
            listOf(
                "id", "vehicle_id", "type", "occurred_at", "odometer_meters", "created_at",
                "distance_meters", "logged_odometer_meters", "occurred_zone", "occurred_offset_seconds",
            ),
            columns(fresh, "vehicle_event"),
        )
    }
}
