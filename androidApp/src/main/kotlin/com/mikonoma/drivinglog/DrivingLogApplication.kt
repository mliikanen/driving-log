package com.mikonoma.drivinglog

import android.app.Application
import android.text.format.DateFormat
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.di.createAppGraph
import com.mikonoma.drivinglog.locale.SystemDeviceLocale
import com.mikonoma.drivinglog.vehicle.data.DatabaseDriverFactory

class DrivingLogApplication : Application() {
    val graph: AppGraph by lazy {
        createAppGraph(
            driver = DatabaseDriverFactory(this).createDriver(),
            // Read on every call, so the system's 12/24-hour setting is followed without restarting.
            deviceLocale = SystemDeviceLocale { DateFormat.is24HourFormat(this) },
        )
    }
}
