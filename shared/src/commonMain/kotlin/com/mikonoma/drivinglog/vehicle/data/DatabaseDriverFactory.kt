package com.mikonoma.drivinglog.vehicle.data

import app.cash.sqldelight.db.SqlDriver

/** Creates the SQLite driver for the real database. Each platform has its own constructor. */
expect class DatabaseDriverFactory {
    fun createDriver(databaseName: String = DATABASE_NAME): SqlDriver
}

const val DATABASE_NAME = "driving-log.db"
