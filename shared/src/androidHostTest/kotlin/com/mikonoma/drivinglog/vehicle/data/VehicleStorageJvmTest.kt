package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.vehicle.domain.Distance
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import java.io.File
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleStorageJvmTest {

    private val originalLocale = Locale.getDefault()
    private val tempFiles = mutableListOf<File>()

    @AfterTest
    fun cleanUp() {
        Locale.setDefault(originalLocale)
        tempFiles.forEach { it.delete() }
    }

    private fun repository(driver: SqlDriver) = SqlDelightVehicleRepository(
        database = DrivingLogDatabase(driver),
        clock = FakeClock(Instant.fromEpochMilliseconds(1_700_000_000_000)),
        newId = generateSequence(1) { it + 1 }.map { "id-$it" }.iterator().let { ids -> { ids.next() } },
        dispatcher = UnconfinedTestDispatcher(),
        deviceTimeZone = com.mikonoma.drivinglog.vehicle.FixedDeviceTimeZone(),
        pictures = com.mikonoma.drivinglog.vehicle.picture.FakeVehiclePictureStore(),
    )

    @Test
    fun aVehicleAndItsEventSurviveReopeningTheDatabaseFile() = runTest {
        val file = File.createTempFile("driving-log-test", ".db").also { tempFiles += it }
        val url = "jdbc:sqlite:${file.absolutePath}"

        // The empty file is a brand new database: create the schema, add a vehicle, close it.
        val first = JdbcSqliteDriver(url).also { DrivingLogDatabase.Schema.create(it) }
        val id = repository(first).addVehicle("Family car", "ABC-123", OdometerUnit.KILOMETERS_TENTHS, Distance(45_200_300))
        first.close()

        // Reopen the same file without creating the schema again.
        val second = JdbcSqliteDriver(url)
        val reopened = repository(second)
        val vehicle = reopened.observeVehicles().first().single()
        assertEquals(id, vehicle.id)
        assertEquals("Family car", vehicle.name)
        assertEquals("ABC-123", vehicle.licensePlate)
        assertEquals(OdometerUnit.KILOMETERS_TENTHS, vehicle.odometerUnit)
        val event = reopened.observeLog(id).first().single() as VehicleEvent.InitialOdometer
        assertEquals(Distance(45_200_300), event.reading)
        second.close()
    }

    private fun rows(driver: SqlDriver, sql: String, columns: Int): List<List<Any?>> =
        driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                val rows = mutableListOf<List<Any?>>()
                while (cursor.next().value) {
                    // Read every column as text so the raw stored form can be compared and inspected.
                    rows += (0 until columns).map { i -> cursor.getString(i) }
                }
                QueryResult.Value(rows)
            },
            parameters = 0,
        ).value

    /** Types 1, 2, 3, 5 into a tenths odometer and saves the vehicle, all under [locale]. Returns the raw stored rows. */
    private fun storedRowsUnder(locale: String): List<List<Any?>> {
        Locale.setDefault(Locale.forLanguageTag(locale))
        val driver = createTestDriver()
        try {
            val entry = OdometerEntry(OdometerUnit.KILOMETERS_TENTHS).applyEdit("1235")
            kotlinx.coroutines.runBlocking {
                repository(driver).addVehicle("Van", "X1", entry.unit, checkNotNull(entry.toDistance()))
            }
            return rows(driver, "SELECT id, name, license_plate, odometer_unit, created_at, updated_at FROM vehicle", 6) +
                rows(driver, "SELECT id, vehicle_id, type, occurred_at, odometer_meters, created_at FROM vehicle_event", 6)
        } finally {
            driver.close()
        }
    }

    @Test
    fun theStoredRowsAreTheSameUnderFinnishAndEnglishLocales() {
        val finnish = storedRowsUnder("fi-FI")
        val english = storedRowsUnder("en-US")

        assertEquals(finnish, english)
    }

    @Test
    fun onlyIntegersUuidStyleIdsAndEnumCodesAreStored() {
        val rows = storedRowsUnder("fi-FI")
        val vehicle = rows[0]
        val event = rows[1]

        assertEquals("KILOMETERS_TENTHS", vehicle[3])
        assertEquals("1700000000000", vehicle[4])
        assertEquals("INITIAL_ODOMETER", event[2])
        assertEquals("123500", event[4]) // 123.5 km typed as 1, 2, 3, 5, stored as whole meters
        val text = rows.flatten().filterNotNull().joinToString(" ")
        assertTrue(',' !in text.replace("Van", "").replace("X1", ""), "no formatted numbers: $text")
    }
}
