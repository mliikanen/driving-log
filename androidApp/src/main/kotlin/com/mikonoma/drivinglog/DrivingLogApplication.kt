package com.mikonoma.drivinglog

import android.app.Application
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.di.createAppGraph
import com.mikonoma.drivinglog.vehicle.data.DatabaseDriverFactory

class DrivingLogApplication : Application() {
    val graph: AppGraph by lazy { createAppGraph(DatabaseDriverFactory(this).createDriver()) }
}
