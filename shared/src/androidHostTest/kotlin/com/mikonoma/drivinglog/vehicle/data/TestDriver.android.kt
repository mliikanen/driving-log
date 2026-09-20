package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase

actual fun createTestDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { DrivingLogDatabase.Schema.create(it) }
