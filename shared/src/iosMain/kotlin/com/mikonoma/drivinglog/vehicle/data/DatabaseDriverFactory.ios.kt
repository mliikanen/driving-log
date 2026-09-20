package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(DrivingLogDatabase.Schema, DATABASE_NAME)
}
