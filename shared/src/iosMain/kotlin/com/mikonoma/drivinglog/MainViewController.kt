package com.mikonoma.drivinglog

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.di.createAppGraph
import platform.UIKit.UIViewController

private val appGraph: AppGraph by lazy { createAppGraph() }

fun MainViewController(): UIViewController = ComposeUIViewController {
    val homeProcessor = remember { appGraph.homeProcessor }
    App(homeProcessor)
}
