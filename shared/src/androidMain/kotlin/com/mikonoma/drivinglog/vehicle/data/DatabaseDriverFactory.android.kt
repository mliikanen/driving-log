package com.mikonoma.drivinglog.vehicle.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(DrivingLogDatabase.Schema, context, DATABASE_NAME)
}
