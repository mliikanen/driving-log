package com.mikonoma.drivinglog.vehicle.data

import com.mikonoma.drivinglog.vehicle.domain.VehicleColors
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.vehicle.FixedDeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.EventZone
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleFuelType
import com.mikonoma.drivinglog.vehicle.domain.ZonedMoment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
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

/** The schema as it was in version 2: version 1 plus distance entries and time zones on events. */
private val VERSION_2_SCHEMA = VERSION_1_SCHEMA + listOf(
    "ALTER TABLE vehicle_event ADD COLUMN distance_meters INTEGER",
    "ALTER TABLE vehicle_event ADD COLUMN logged_odometer_meters INTEGER",
    "ALTER TABLE vehicle_event ADD COLUMN occurred_zone TEXT",
    "ALTER TABLE vehicle_event ADD COLUMN occurred_offset_seconds INTEGER",
)

/** The schema as it was in version 3: version 2 plus the remembered tenths choice on the vehicle. */
private val VERSION_3_SCHEMA = VERSION_2_SCHEMA + listOf(
    "ALTER TABLE vehicle ADD COLUMN log_distance_tenths INTEGER",
)

/** The schema as it was in version 4: version 3 plus the vehicle's picture id. */
private val VERSION_4_SCHEMA = VERSION_3_SCHEMA + listOf(
    "ALTER TABLE vehicle ADD COLUMN picture_id TEXT",
)

/** The schema as it was in version 5: version 4 plus the vehicle's type. */
private val VERSION_5_SCHEMA = VERSION_4_SCHEMA + listOf(
    "ALTER TABLE vehicle ADD COLUMN vehicle_type TEXT NOT NULL DEFAULT 'CAR'",
)

/** The schema as it was in version 6: version 5 plus the vehicle's color. No app_state table yet. */
private val VERSION_6_SCHEMA = VERSION_5_SCHEMA + listOf(
    "ALTER TABLE vehicle ADD COLUMN vehicle_color TEXT NOT NULL DEFAULT '203A43'",
)

/** The schema as it was in version 7: version 6 plus the app_state table. No note column on events yet. */
private val VERSION_7_SCHEMA = VERSION_6_SCHEMA + listOf(
    """
    CREATE TABLE app_state (
        key   TEXT NOT NULL PRIMARY KEY,
        value TEXT NOT NULL
    )
    """,
)

/** The schema as it was in version 8: version 7 plus the note column (add-event-notes). */
private val VERSION_8_SCHEMA = VERSION_7_SCHEMA + listOf(
    "ALTER TABLE vehicle_event ADD COLUMN note TEXT",
)

/** The schema as it was in version 9: version 8 plus the photos of an event (add-event-pictures). */
private val VERSION_9_SCHEMA = VERSION_8_SCHEMA + listOf(
    """
    CREATE TABLE event_picture (
        id          TEXT    NOT NULL PRIMARY KEY,
        event_id    TEXT    NOT NULL REFERENCES vehicle_event(id),
        position    INTEGER NOT NULL,
        created_at  INTEGER NOT NULL
    )
    """,
    "CREATE INDEX event_picture_by_event ON event_picture (event_id, position)",
)

/** The schema as it was in version 10: version 9 plus the scan a reading came from (add-odometer-ocr-capture). */
private val VERSION_10_SCHEMA = VERSION_9_SCHEMA + listOf(
    """
    CREATE TABLE event_capture (
        event_id    TEXT    NOT NULL PRIMARY KEY,
        photo_id    TEXT    NOT NULL,
        detections  TEXT    NOT NULL
    )
    """,
)

/** The schema as it was in version 11: version 10 plus refueling events (add-refueling-logging). */
private val VERSION_11_SCHEMA = VERSION_10_SCHEMA + listOf(
    "ALTER TABLE vehicle_event ADD COLUMN fuel_amount_milliliters INTEGER",
    "ALTER TABLE vehicle_event ADD COLUMN fuel_unit TEXT",
    "ALTER TABLE vehicle_event ADD COLUMN fuel_type TEXT",
    "ALTER TABLE vehicle_event ADD COLUMN filled_up INTEGER",
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
        pictures = com.mikonoma.drivinglog.vehicle.picture.FakePictureStore(),
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

        DrivingLogDatabase.Schema.migrate(driver, 1, 12)

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

        DrivingLogDatabase.Schema.migrate(driver, 1, 12)

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
        DrivingLogDatabase.Schema.migrate(driver, 1, 12)

        assertEquals(Distance(45_200_300), repository(driver).observeVehicle("v1").first()?.currentOdometer)
    }

    @Test
    fun aDistanceEntryWithAZoneCanBeAddedAfterMigrating() = runTest {
        val driver = versionOneDatabase()
        DrivingLogDatabase.Schema.migrate(driver, 1, 12)
        val repository = repository(driver)
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T13:00:00Z"), TimeZone.of("America/New_York"))

        repository.addDistanceEntry("v1", moment, Distance(30_000), loggedOdometer = null, tenthsIncluded = true)

        val entry = repository.observeLog("v1").first().first() as VehicleEvent.DistanceEntry
        assertEquals(EventZone("America/New_York", -4 * 3600), entry.occurredAt.zone)
        assertEquals(Distance(45_230_300), repository.observeVehicle("v1").first()?.currentOdometer)
    }

    @Test
    fun aMigratedVehicleCanAlsoGetANewVehicleWithTheDeviceZone() = runTest {
        val driver = versionOneDatabase()
        DrivingLogDatabase.Schema.migrate(driver, 1, 12)
        val repository = repository(driver)

        val id = repository.addVehicle("Van", null, VehicleType.CAR, VehicleColors.default, OdometerUnit.MILES, Distance(1_609_344))

        assertEquals(EventZone("Europe/Helsinki", 3 * 3600), repository.observeLog(id).first().single().occurredAt.zone)
    }

    @Test
    fun aMigratedDatabaseHasTheSameTablesAsAFreshOne() {
        val migrated = versionOneDatabase().also { DrivingLogDatabase.Schema.migrate(it, 1, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
    }

    @Test
    fun aFreshDatabaseIsVersionTwelveWithTheNewColumns() {
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(12L, DrivingLogDatabase.Schema.version)
        assertEquals(
            listOf(
                "id", "vehicle_id", "type", "occurred_at", "odometer_meters", "created_at",
                "distance_meters", "logged_odometer_meters", "occurred_zone", "occurred_offset_seconds", "note",
                "fuel_amount_milliliters", "fuel_unit", "fuel_type", "filled_up",
            ),
            columns(fresh, "vehicle_event"),
        )
        assertEquals(
            listOf(
                "id", "name", "license_plate", "odometer_unit", "created_at", "updated_at", "log_distance_tenths",
                "picture_id", "vehicle_type", "vehicle_color", "vehicle_fuel_type",
            ),
            columns(fresh, "vehicle"),
        )
        assertEquals(listOf("id", "event_id", "position", "created_at"), columns(fresh, "event_picture"))
        assertEquals(listOf("event_id", "photo_id", "detections"), columns(fresh, "event_capture"))
    }

    // ---- Version 2 to 3: the remembered tenths choice

    /** A database as the previous build left it: the version-2 schema with a vehicle and an event with a zone. */
    private fun versionTwoDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_2_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000)", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800)",
            0,
        )
        return driver
    }

    @Test
    fun migratingFromVersionTwoKeepsTheVehicleAndItsZonedEvent() = runTest {
        val driver = versionTwoDatabase()

        DrivingLogDatabase.Schema.migrate(driver, 2, 12)

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        assertNull(vehicle.logDistanceTenths)
        val event = repository.observeLog("v1").first().single()
        assertEquals(EventZone("Europe/Helsinki", 3 * 3600), event.occurredAt.zone)
    }

    @Test
    fun aMigratedVehicleHasNoRememberedTenthsChoiceFromEitherOlderVersion() = runTest {
        val fromOne = versionOneDatabase().also { DrivingLogDatabase.Schema.migrate(it, 1, 12) }
        val fromTwo = versionTwoDatabase().also { DrivingLogDatabase.Schema.migrate(it, 2, 12) }

        assertNull(repository(fromOne).observeVehicles().first().single().logDistanceTenths)
        assertNull(repository(fromTwo).observeVehicles().first().single().logDistanceTenths)
    }

    @Test
    fun theTenthsChoiceCanBeRememberedAfterMigratingFromVersionTwo() = runTest {
        val driver = versionTwoDatabase()
        DrivingLogDatabase.Schema.migrate(driver, 2, 12)
        val repository = repository(driver)
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T13:00:00Z"), TimeZone.of("Europe/Helsinki"))

        repository.addDistanceEntry("v1", moment, Distance(30_000), loggedOdometer = null, tenthsIncluded = true)

        assertEquals(true, repository.observeVehicles().first().single().logDistanceTenths)
    }

    @Test
    fun aVersionTwoDatabaseMigratedToSixHasTheSameTablesAsAFreshOne() {
        val migrated = versionTwoDatabase().also { DrivingLogDatabase.Schema.migrate(it, 2, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
    }

    // ---- Version 3 to 4: the vehicle's picture

    /** A database as the previous build left it: the version-3 schema with a vehicle that remembers its tenths choice. */
    private fun versionThreeDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_3_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1)", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800)",
            0,
        )
        return driver
    }

    @Test
    fun migratingFromVersionThreeKeepsTheVehicleItsTenthsChoiceAndItsEvent() = runTest {
        val driver = versionThreeDatabase()

        DrivingLogDatabase.Schema.migrate(driver, 3, 12)

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        assertEquals(true, vehicle.logDistanceTenths)
        assertEquals(EventZone("Europe/Helsinki", 3 * 3600), repository.observeLog("v1").first().single().occurredAt.zone)
    }

    @Test
    fun aMigratedVehicleHasNoPictureFromEveryOlderVersion() = runTest {
        val fromOne = versionOneDatabase().also { DrivingLogDatabase.Schema.migrate(it, 1, 12) }
        val fromTwo = versionTwoDatabase().also { DrivingLogDatabase.Schema.migrate(it, 2, 12) }
        val fromThree = versionThreeDatabase().also { DrivingLogDatabase.Schema.migrate(it, 3, 12) }

        for (driver in listOf(fromOne, fromTwo, fromThree)) {
            val details = repository(driver).observeVehicle("v1").first()!!
            assertNull(details.vehicle.pictureId)
            assertNull(repository(driver).observeVehicles().first().single().pictureId)
        }
    }

    @Test
    fun aMigratedVehicleCanGetAPictureIdAndTheOdometerIsStillItsReading() = runTest {
        val driver = versionThreeDatabase().also { DrivingLogDatabase.Schema.migrate(it, 3, 12) }
        DrivingLogDatabase(driver).vehicleQueries.updateVehiclePicture("pic-1", 2000, "v1")

        val repository = repository(driver)
        assertEquals("pic-1", repository.observeVehicle("v1").first()!!.vehicle.pictureId)
        assertEquals("pic-1", repository.observeVehicles().first().single().pictureId)
        assertEquals(Distance(45_200_000), repository.observeVehicle("v1").first()!!.currentOdometer)
    }

    @Test
    fun aVersionThreeDatabaseMigratedToSixHasTheSameTablesAsAFreshOne() {
        val migrated = versionThreeDatabase().also { DrivingLogDatabase.Schema.migrate(it, 3, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
    }

    // ---- Version 4 to 5: the vehicle's type

    /** A database as the previous build left it: the version-4 schema with a vehicle that has a picture. */
    private fun versionFourDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_4_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1, 'pic-1')", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800)",
            0,
        )
        return driver
    }

    @Test
    fun migratingFromVersionFourKeepsTheVehicleItsPictureAndItsEvent() = runTest {
        val driver = versionFourDatabase()

        DrivingLogDatabase.Schema.migrate(driver, 4, 12)

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        assertEquals("pic-1", vehicle.pictureId)
        assertEquals(true, vehicle.logDistanceTenths)
        assertEquals(1, repository.observeLog("v1").first().size)
    }

    @Test
    fun aMigratedVehicleIsACarFromEveryOlderVersion() = runTest {
        val drivers = listOf(
            versionOneDatabase().also { DrivingLogDatabase.Schema.migrate(it, 1, 12) },
            versionTwoDatabase().also { DrivingLogDatabase.Schema.migrate(it, 2, 12) },
            versionThreeDatabase().also { DrivingLogDatabase.Schema.migrate(it, 3, 12) },
            versionFourDatabase().also { DrivingLogDatabase.Schema.migrate(it, 4, 12) },
        )

        for (driver in drivers) {
            assertEquals(VehicleType.CAR, repository(driver).observeVehicle("v1").first()!!.vehicle.type)
            assertEquals(VehicleType.CAR, repository(driver).observeVehicles().first().single().type)
        }
    }

    @Test
    fun aMigratedVehicleCanGetATypeAndItsOdometerIsStillItsReading() = runTest {
        val driver = versionFourDatabase().also { DrivingLogDatabase.Schema.migrate(it, 4, 12) }
        DrivingLogDatabase(driver).vehicleQueries.updateVehicleType("VAN", 2000, "v1")

        val repository = repository(driver)
        assertEquals(VehicleType.VAN, repository.observeVehicle("v1").first()!!.vehicle.type)
        assertEquals(Distance(45_200_000), repository.observeVehicle("v1").first()!!.currentOdometer)
    }

    @Test
    fun aStoredCodeThisAppDoesNotKnowReadsAsOtherAfterMigrating() = runTest {
        val driver = versionFourDatabase().also { DrivingLogDatabase.Schema.migrate(it, 4, 12) }
        DrivingLogDatabase(driver).vehicleQueries.updateVehicleType("HOVERCRAFT", 2000, "v1")

        assertEquals(VehicleType.OTHER, repository(driver).observeVehicles().first().single().type)
    }

    @Test
    fun theDatabaseRejectsAVehicleWithoutAType() {
        for (driver in listOf(
            memoryDriver().also { DrivingLogDatabase.Schema.create(it) },
            versionFourDatabase().also { DrivingLogDatabase.Schema.migrate(it, 4, 12) },
        )) {
            assertFails {
                driver.execute(
                    null,
                    "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at, vehicle_type) VALUES ('x', 'No type', 'KILOMETERS', 1, 1, NULL)",
                    0,
                )
            }
            driver.execute(null, "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at, vehicle_type) VALUES ('ok', 'Has type', 'KILOMETERS', 1, 1, 'VAN')", 0)
            assertFails { driver.execute(null, "UPDATE vehicle SET vehicle_type = NULL WHERE id = 'ok'", 0) }
        }
    }

    @Test
    fun aVehicleInsertedWithoutSayingATypeIsACar() = runTest {
        val driver = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        driver.execute(null, "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at) VALUES ('x', 'Default', 'KILOMETERS', 1, 1)", 0)

        assertEquals(VehicleType.CAR, repository(driver).observeVehicles().first().single().type)
    }

    @Test
    fun aVersionFourDatabaseMigratedToSevenHasTheSameTablesAsAFreshOne() {
        val migrated = versionFourDatabase().also { DrivingLogDatabase.Schema.migrate(it, 4, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
    }

    // ---- Version 5 to 6: the vehicle's color

    /** A database as the previous build left it: the version-5 schema with a vehicle that has a picture and the type Van. */
    private fun versionFiveDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_5_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1, 'pic-1', 'VAN')", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800)",
            0,
        )
        return driver
    }

    // ---- Version 6 to 7: the application's own state (the vehicle last logged for)

    /** A database as the previous build left it: the version-6 schema with a vehicle that has a picture, a type and a color. */
    private fun versionSixDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_6_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1, 'pic-1', 'VAN', 'E53935')", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800)",
            0,
        )
        return driver
    }

    @Test
    fun migratingFromVersionSixKeepsTheVehicleItsColorAndItsEventAndAddsAnEmptyAppStateTable() = runTest {
        val driver = versionSixDatabase().also { DrivingLogDatabase.Schema.migrate(it, 6, 12) }

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        assertEquals(com.mikonoma.drivinglog.vehicle.domain.Rgb(0xE53935), vehicle.color)
        assertEquals(1, repository.observeLog("v1").first().size)
        // No row: nothing is derived from the events for the memory this table holds (see add-direct-logging's design).
        assertEquals(
            null,
            driver.executeQuery(null, "SELECT value FROM app_state WHERE key = 'last_logged_vehicle_id'", { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
            }, 0).value,
        )
    }

    @Test
    fun aVersionSixDatabaseMigratedToEightHasTheSameTablesAsAFreshOneIncludingAppState() {
        val migrated = versionSixDatabase().also { DrivingLogDatabase.Schema.migrate(it, 6, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
        assertEquals(columns(fresh, "app_state"), columns(migrated, "app_state"))
    }

    // ---- Version 7 to 8: a note on a distance entry or an odometer anchor

    /** A database as the previous build left it: the version-7 schema (app_state exists, empty) with a vehicle and its initial event. */
    private fun versionSevenDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_7_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1, 'pic-1', 'VAN', 'E53935')", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800)",
            0,
        )
        return driver
    }

    @Test
    fun migratingFromVersionSevenKeepsTheVehicleItsColorAndItsEventAndAddsAnEmptyNoteColumn() = runTest {
        val driver = versionSevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 7, 12) }

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        assertEquals(com.mikonoma.drivinglog.vehicle.domain.Rgb(0xE53935), vehicle.color)
        val event = repository.observeLog("v1").first().single() as VehicleEvent.InitialOdometer
        assertEquals(Distance(45_200_000), event.reading)
        assertNull(
            driver.executeQuery(null, "SELECT note FROM vehicle_event WHERE id = 'e1'", { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getString(0))
            }, 0).value,
        )
    }

    @Test
    fun aVersionSevenDatabaseMigratedToEightHasTheSameTablesAsAFreshOneIncludingTheNoteColumn() {
        val migrated = versionSevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 7, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
        assertEquals(columns(fresh, "app_state"), columns(migrated, "app_state"))
        assertEquals(
            listOf(
                "id", "vehicle_id", "type", "occurred_at", "odometer_meters", "created_at", "distance_meters",
                "logged_odometer_meters", "occurred_zone", "occurred_offset_seconds", "note",
                "fuel_amount_milliliters", "fuel_unit", "fuel_type", "filled_up",
            ),
            columns(migrated, "vehicle_event"),
        )
    }

    @Test
    fun aDistanceEntryWithANoteCanBeAddedAfterMigratingFromVersionSeven() = runTest {
        val driver = versionSevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 7, 12) }
        val repository = repository(driver)
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T13:00:00Z"), TimeZone.of("Europe/Helsinki"))

        repository.addDistanceEntry("v1", moment, Distance(30_000), loggedOdometer = null, tenthsIncluded = false, note = "borrowed to Sam")

        val entry = repository.observeLog("v1").first().first() as VehicleEvent.DistanceEntry
        assertEquals("borrowed to Sam", entry.note)
    }

    // ---- Version 8 to 9: photos on a distance entry or an odometer anchor (add-event-pictures)

    /** A database as the previous build left it: the version-8 schema (note column exists, empty) with a vehicle and its initial event. */
    private fun versionEightDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_8_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1, 'pic-1', 'VAN', 'E53935')", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds, note) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800, NULL)",
            0,
        )
        return driver
    }

    @Test
    fun migratingFromVersionEightKeepsTheVehicleItsNoteAndAddsTheEventPictureTable() = runTest {
        val driver = versionEightDatabase().also { DrivingLogDatabase.Schema.migrate(it, 8, 12) }

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        val event = repository.observeLog("v1").first().single() as VehicleEvent.InitialOdometer
        assertEquals(Distance(45_200_000), event.reading)
        assertEquals(emptyList(), event.photoIds)
    }

    @Test
    fun aVersionEightDatabaseMigratedToNineHasTheSameTablesAsAFreshOneIncludingEventPicture() {
        val migrated = versionEightDatabase().also { DrivingLogDatabase.Schema.migrate(it, 8, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
        assertEquals(columns(fresh, "app_state"), columns(migrated, "app_state"))
        assertEquals(listOf("id", "event_id", "position", "created_at"), columns(migrated, "event_picture"))
    }

    @Test
    fun aDistanceEntryWithAPhotoCanBeAddedAfterMigratingFromVersionEight() = runTest {
        val driver = versionEightDatabase().also { DrivingLogDatabase.Schema.migrate(it, 8, 12) }
        val pictureStore = com.mikonoma.drivinglog.vehicle.picture.FakePictureStore()
        val repository = SqlDelightVehicleRepository(
            database = DrivingLogDatabase(driver),
            clock = com.mikonoma.drivinglog.vehicle.data.FakeClock(Instant.parse("2026-09-20T12:00:00Z")),
            newId = generateSequence(1) { it + 1 }.map { "new-$it" }.iterator().let { ids -> { ids.next() } },
            dispatcher = UnconfinedTestDispatcher(),
            deviceTimeZone = FixedDeviceTimeZone(TimeZone.of("Europe/Helsinki")),
            pictures = pictureStore,
        )
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T13:00:00Z"), TimeZone.of("Europe/Helsinki"))
        val pending = com.mikonoma.drivinglog.vehicle.domain.PendingPicture(pictureStore.addPending())

        repository.addDistanceEntry("v1", moment, Distance(30_000), loggedOdometer = null, tenthsIncluded = false, photos = listOf(pending))

        val entry = repository.observeLog("v1").first().first() as VehicleEvent.DistanceEntry
        assertEquals(1, entry.photoIds.size)
    }

    // ---- Version 9 to 10: the scan an event's number came from (add-odometer-ocr-capture)

    /** A database as the previous build left it: the version-9 schema with a vehicle, its initial event and an event photo. */
    private fun versionNineDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_9_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1, NULL, 'CAR', 'E53935')", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds, note) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800, NULL)",
            0,
        )
        driver.execute(null, "INSERT INTO event_picture VALUES ('p1', 'e1', 0, 1785067200000)", 0)
        return driver
    }

    @Test
    fun migratingFromVersionNineKeepsTheEventAndItsPhotoRowAndAddsTheCaptureTable() = runTest {
        val driver = versionNineDatabase().also { DrivingLogDatabase.Schema.migrate(it, 9, 12) }

        val repository = repository(driver)
        val event = repository.observeLog("v1").first().single() as VehicleEvent.InitialOdometer
        assertEquals(Distance(45_200_000), event.reading)
        assertEquals(listOf("p1"), DrivingLogDatabase(driver).eventPictureQueries.selectPhotoIdsForEvent("e1").executeAsList())
        assertNull(repository.captureOf("e1"))
    }

    @Test
    fun aVersionNineDatabaseMigratedToTenHasTheSameTablesAsAFreshOne() {
        val migrated = versionNineDatabase().also { DrivingLogDatabase.Schema.migrate(it, 9, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        for (table in listOf("vehicle", "vehicle_event", "app_state", "event_picture", "event_capture")) {
            assertEquals(columns(fresh, table), columns(migrated, table), table)
        }
    }

    // ---- Version 10 to 11: refueling events (add-refueling-logging)

    /** A database as the previous build left it: the version-10 schema with a vehicle and an initial event. */
    private fun versionTenDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_10_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1, NULL, 'CAR', 'E53935')", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds, note) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800, NULL)",
            0,
        )
        return driver
    }

    @Test
    fun migratingFromVersionTenKeepsTheVehicleAndItsEvent() = runTest {
        val driver = versionTenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 10, 12) }

        val repository = repository(driver)
        val event = repository.observeLog("v1").first().single() as VehicleEvent.InitialOdometer
        assertEquals(Distance(45_200_000), event.reading)
    }

    @Test
    fun aVersionTenDatabaseMigratedToElevenHasTheSameTablesAsAFreshOne() {
        val migrated = versionTenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 10, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        for (table in listOf("vehicle", "vehicle_event", "app_state", "event_picture", "event_capture")) {
            assertEquals(columns(fresh, table), columns(migrated, table), table)
        }
    }

    @Test
    fun aRefuelingCanBeAddedAfterMigratingFromVersionTen() = runTest {
        val driver = versionTenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 10, 12) }
        val repository = repository(driver)
        val moment = ZonedMoment.of(Instant.parse("2026-09-20T13:00:00Z"), TimeZone.of("Europe/Helsinki"))

        repository.addRefueling("v1", moment, com.mikonoma.drivinglog.vehicle.domain.Volume(42_300), com.mikonoma.drivinglog.vehicle.domain.FuelUnit.LITERS, com.mikonoma.drivinglog.vehicle.domain.FuelType.DIESEL, filledUp = true)

        val refueling = repository.observeLog("v1").first().first() as VehicleEvent.Refueling
        assertEquals(com.mikonoma.drivinglog.vehicle.domain.Volume(42_300), refueling.amount)
    }

    // ---- Version 11 to 12: the vehicle's fuel type (vehicle-fuel-type)

    /** A database as the previous build left it: the version-11 schema (refueling columns exist) with a vehicle and an initial event. */
    private fun versionElevenDatabase(): SqlDriver {
        val driver = memoryDriver()
        VERSION_11_SCHEMA.forEach { driver.execute(null, it.trimIndent(), 0) }
        driver.execute(null, "INSERT INTO vehicle VALUES ('v1', 'Family car', 'ABC-123', 'KILOMETERS', 1000, 1000, 1, NULL, 'CAR', 'E53935')", 0)
        driver.execute(
            null,
            "INSERT INTO vehicle_event (id, vehicle_id, type, occurred_at, odometer_meters, created_at, occurred_zone, occurred_offset_seconds, note) " +
                "VALUES ('e1', 'v1', 'INITIAL_ODOMETER', 1785067200000, 45200000, 1785067200000, 'Europe/Helsinki', 10800, NULL)",
            0,
        )
        return driver
    }

    @Test
    fun migratingFromVersionElevenKeepsTheVehicleAndItsEvent() = runTest {
        val driver = versionElevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 11, 12) }

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        val event = repository.observeLog("v1").first().single() as VehicleEvent.InitialOdometer
        assertEquals(Distance(45_200_000), event.reading)
    }

    @Test
    fun aMigratedVehicleIsPetrolFromEveryOlderVersion() = runTest {
        val drivers = listOf(
            versionOneDatabase().also { DrivingLogDatabase.Schema.migrate(it, 1, 12) },
            versionTenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 10, 12) },
            versionElevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 11, 12) },
        )

        for (driver in drivers) {
            assertEquals(VehicleFuelType.PETROL, repository(driver).observeVehicle("v1").first()!!.vehicle.fuelType)
            assertEquals(VehicleFuelType.PETROL, repository(driver).observeVehicles().first().single().fuelType)
        }
    }

    @Test
    fun aMigratedVehicleCanGetAFuelTypeAndItsOdometerIsStillItsReading() = runTest {
        val driver = versionElevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 11, 12) }
        DrivingLogDatabase(driver).vehicleQueries.updateVehicleFuelType("DIESEL", 2000, "v1")

        val repository = repository(driver)
        assertEquals(VehicleFuelType.DIESEL, repository.observeVehicle("v1").first()!!.vehicle.fuelType)
        assertEquals(Distance(45_200_000), repository.observeVehicle("v1").first()!!.currentOdometer)
    }

    @Test
    fun aStoredFuelTypeCodeThisAppDoesNotKnowReadsAsOtherAfterMigrating() = runTest {
        val driver = versionElevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 11, 12) }
        DrivingLogDatabase(driver).vehicleQueries.updateVehicleFuelType("ELECTRIC", 2000, "v1")

        assertEquals(VehicleFuelType.OTHER, repository(driver).observeVehicles().first().single().fuelType)
    }

    @Test
    fun theDatabaseRejectsAVehicleWithoutAFuelType() {
        for (driver in listOf(
            memoryDriver().also { DrivingLogDatabase.Schema.create(it) },
            versionElevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 11, 12) },
        )) {
            assertFails {
                driver.execute(
                    null,
                    "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at, vehicle_fuel_type) VALUES ('x', 'No fuel type', 'KILOMETERS', 1, 1, NULL)",
                    0,
                )
            }
            driver.execute(null, "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at, vehicle_fuel_type) VALUES ('ok', 'Has fuel type', 'KILOMETERS', 1, 1, 'LPG')", 0)
            assertFails { driver.execute(null, "UPDATE vehicle SET vehicle_fuel_type = NULL WHERE id = 'ok'", 0) }
        }
    }

    @Test
    fun aVehicleInsertedWithoutSayingAFuelTypeIsPetrol() = runTest {
        val driver = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        driver.execute(null, "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at) VALUES ('x', 'Default', 'KILOMETERS', 1, 1)", 0)

        assertEquals(VehicleFuelType.PETROL, repository(driver).observeVehicles().first().single().fuelType)
    }

    @Test
    fun aVersionElevenDatabaseMigratedToTwelveHasTheSameTablesAsAFreshOne() {
        val migrated = versionElevenDatabase().also { DrivingLogDatabase.Schema.migrate(it, 11, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        for (table in listOf("vehicle", "vehicle_event", "app_state", "event_picture", "event_capture")) {
            assertEquals(columns(fresh, table), columns(migrated, table), table)
        }
    }

    private fun migratedFromEveryOlderVersion(): List<SqlDriver> = listOf(
        versionOneDatabase().also { DrivingLogDatabase.Schema.migrate(it, 1, 12) },
        versionTwoDatabase().also { DrivingLogDatabase.Schema.migrate(it, 2, 12) },
        versionThreeDatabase().also { DrivingLogDatabase.Schema.migrate(it, 3, 12) },
        versionFourDatabase().also { DrivingLogDatabase.Schema.migrate(it, 4, 12) },
        versionFiveDatabase().also { DrivingLogDatabase.Schema.migrate(it, 5, 12) },
    )

    @Test
    fun migratingFromVersionFiveKeepsTheVehicleItsTypeItsPictureAndItsEvent() = runTest {
        val driver = versionFiveDatabase().also { DrivingLogDatabase.Schema.migrate(it, 5, 12) }

        val repository = repository(driver)
        val vehicle = repository.observeVehicles().first().single()
        assertEquals("Family car", vehicle.name)
        assertEquals(VehicleType.VAN, vehicle.type)
        assertEquals("pic-1", vehicle.pictureId)
        assertEquals(Distance(45_200_000), repository.observeVehicle("v1").first()!!.currentOdometer)
        assertEquals(1, repository.observeLog("v1").first().size)
    }

    @Test
    fun aMigratedVehicleHasTheDefaultColorFromEveryOlderVersion() = runTest {
        for (driver in migratedFromEveryOlderVersion()) {
            assertEquals(VehicleColors.default, repository(driver).observeVehicle("v1").first()!!.vehicle.color)
            assertEquals(VehicleColors.default, repository(driver).observeVehicles().first().single().color)
        }
    }

    @Test
    fun aMigratedVehicleIsNotGivenAColorFromItsPicture() = runTest {
        // The version-5 vehicle has a picture; there is no backfill, so it has the default until the user acts.
        val driver = versionFiveDatabase().also { DrivingLogDatabase.Schema.migrate(it, 5, 12) }

        assertEquals(VehicleColors.default, repository(driver).observeVehicles().first().single().color)
    }

    @Test
    fun aMigratedVehicleCanGetAColorAndItsOdometerIsStillItsReading() = runTest {
        val driver = versionFiveDatabase().also { DrivingLogDatabase.Schema.migrate(it, 5, 12) }
        DrivingLogDatabase(driver).vehicleQueries.updateVehicleColor("E53935", 2000, "v1")

        val repository = repository(driver)
        assertEquals(com.mikonoma.drivinglog.vehicle.domain.Rgb(0xE53935), repository.observeVehicle("v1").first()!!.vehicle.color)
        assertEquals(Distance(45_200_000), repository.observeVehicle("v1").first()!!.currentOdometer)
    }

    @Test
    fun aStoredValueThatIsNotAColorReadsAsTheDefault() = runTest {
        val driver = versionFiveDatabase().also { DrivingLogDatabase.Schema.migrate(it, 5, 12) }

        for (bad in listOf("", "red", "#E53935", "E5393", "E539355", "GGGGGG")) {
            DrivingLogDatabase(driver).vehicleQueries.updateVehicleColor(bad, 2000, "v1")
            assertEquals(VehicleColors.default, repository(driver).observeVehicles().first().single().color, "'$bad'")
            assertEquals(VehicleColors.default, repository(driver).observeVehicle("v1").first()!!.vehicle.color, "'$bad'")
        }
    }

    @Test
    fun theDatabaseRejectsAVehicleWithoutAColor() {
        for (driver in listOf(
            memoryDriver().also { DrivingLogDatabase.Schema.create(it) },
            versionFiveDatabase().also { DrivingLogDatabase.Schema.migrate(it, 5, 12) },
        )) {
            assertFails {
                driver.execute(
                    null,
                    "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at, vehicle_color) VALUES ('x', 'No color', 'KILOMETERS', 1, 1, NULL)",
                    0,
                )
            }
            driver.execute(null, "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at, vehicle_color) VALUES ('ok', 'Has color', 'KILOMETERS', 1, 1, 'E53935')", 0)
            assertFails { driver.execute(null, "UPDATE vehicle SET vehicle_color = NULL WHERE id = 'ok'", 0) }
        }
    }

    @Test
    fun aVehicleInsertedWithoutSayingAColorHasTheDefault() = runTest {
        val driver = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        driver.execute(null, "INSERT INTO vehicle (id, name, odometer_unit, created_at, updated_at) VALUES ('x', 'Default', 'KILOMETERS', 1, 1)", 0)

        assertEquals(VehicleColors.default, repository(driver).observeVehicles().first().single().color)
    }

    @Test
    fun theColumnDefaultInTheSchemaIsTheDefaultColorOfTheApp() {
        // A migration cannot call Kotlin, so the default is a literal in the SQL; it must be the constant, in a fresh and in a migrated database.
        for (driver in listOf(
            memoryDriver().also { DrivingLogDatabase.Schema.create(it) },
            versionFiveDatabase().also { DrivingLogDatabase.Schema.migrate(it, 5, 12) },
        )) {
            val default = driver.executeQuery(
                identifier = null,
                sql = "SELECT dflt_value FROM pragma_table_info('vehicle') WHERE name = 'vehicle_color'",
                mapper = { cursor ->
                    cursor.next()
                    QueryResult.Value(cursor.getString(0))
                },
                parameters = 0,
            ).value
            assertEquals("'${VehicleColors.default.hex}'", default)
        }
    }

    @Test
    fun aVersionFiveDatabaseMigratedToSevenHasTheSameTablesAsAFreshOne() {
        val migrated = versionFiveDatabase().also { DrivingLogDatabase.Schema.migrate(it, 5, 12) }
        val fresh = memoryDriver().also { DrivingLogDatabase.Schema.create(it) }

        assertEquals(columns(fresh, "vehicle"), columns(migrated, "vehicle"))
        assertEquals(columns(fresh, "vehicle_event"), columns(migrated, "vehicle_event"))
    }
}
