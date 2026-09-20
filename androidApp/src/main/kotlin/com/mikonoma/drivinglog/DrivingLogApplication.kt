package com.mikonoma.drivinglog

import android.app.Application
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.di.createAppGraph

class DrivingLogApplication : Application() {
    val graph: AppGraph by lazy { createAppGraph() }
}
