package com.mikonoma.drivinglog.vehicle.fixtures

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Catches a stale Maestro fixture fast, in this ordinary JVM unit test suite, instead of a Maestro flow failing
 * mysteriously on a device much later. Every checked-in `.db` file under `maestro/assets/fixtures` is only ever
 * valid for the schema version it was generated against (`PRAGMA user_version`, stamped by `GenerateFixturesTest`
 * — see it for why that stamp matters); this test opens every such file and asserts that version still matches
 * the current schema. See docs/test-fixtures.md: whenever this test fails, the fix is always the same — regenerate
 * the fixtures with `./gradlew :shared:generateMaestroFixtures` and check in the result.
 */
class FixtureFreshnessTest {
    @Test
    fun everyCheckedInFixtureMatchesTheCurrentSchema() {
        val fixturesDir = File(
            System.getProperty("maestroFixturesDir") ?: error("system property maestroFixturesDir not set"),
        )
        val fixtures = fixturesDir.listFiles { f -> f.extension == "db" }?.sortedBy { it.name }
            ?: fail("fixtures directory not found: $fixturesDir")
        check(fixtures.isNotEmpty()) { "no fixture .db files found in $fixturesDir" }

        for (fixture in fixtures) {
            val driver = JdbcSqliteDriver("jdbc:sqlite:${fixture.absolutePath}")
            val version = try {
                driver.executeQuery(null, "PRAGMA user_version", { cursor ->
                    cursor.next()
                    QueryResult.Value(cursor.getLong(0) ?: 0L)
                }, 0).value
            } finally {
                driver.close()
            }
            assertEquals(
                DrivingLogDatabase.Schema.version,
                version,
                "${fixture.name} was generated against schema version $version, but the current schema is " +
                    "${DrivingLogDatabase.Schema.version}. Regenerate it: " +
                    "./gradlew :shared:generateMaestroFixtures (see docs/test-fixtures.md).",
            )
        }
    }
}
