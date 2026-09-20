package com.mikonoma.drivinglog

import android.app.Application
import android.text.format.DateFormat
import androidx.compose.runtime.mutableIntStateOf
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.di.createAppGraph
import com.mikonoma.drivinglog.locale.SystemDeviceLocale
import com.mikonoma.drivinglog.vehicle.data.DatabaseDriverFactory

class DrivingLogApplication : Application() {
    /** Bumped when the app returns to the foreground, so screens re-read the system's clock setting. */
    val resumeCount = mutableIntStateOf(0)

    val graph: AppGraph by lazy {
        createAppGraph(
            driver = DatabaseDriverFactory(this).createDriver(),
            // Read on every call; the resume count makes composables that read it recompose after
            // the user changed the system's 12/24-hour setting and came back.
            deviceLocale = SystemDeviceLocale {
                resumeCount.intValue
                DateFormat.is24HourFormat(this)
            },
        )
    }
}
