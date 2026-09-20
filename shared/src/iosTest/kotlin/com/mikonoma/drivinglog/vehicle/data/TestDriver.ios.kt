package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase

actual fun createTestDriver(): SqlDriver = inMemoryDriver(DrivingLogDatabase.Schema)
