package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.SqlDriver

/** A fresh in-memory database with the schema created. Android host tests use JDBC SQLite; iOS tests the native driver. */
expect fun createTestDriver(): SqlDriver
