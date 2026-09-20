package com.mikonoma.drivinglog.di

import com.mikonoma.drivinglog.home.HomeProcessor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(AppScope::class)
interface AppGraph {
    val homeProcessor: HomeProcessor
}

// Metro only rewrites createGraph() in modules with its plugin applied, so the platform shells call this.
fun createAppGraph(): AppGraph = createGraph<AppGraph>()
