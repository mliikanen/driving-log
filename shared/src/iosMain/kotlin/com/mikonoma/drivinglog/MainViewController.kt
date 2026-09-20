package com.mikonoma.drivinglog

import androidx.compose.ui.window.ComposeUIViewController
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.di.createAppGraph
import com.mikonoma.drivinglog.locale.SystemDeviceLocale
import com.mikonoma.drivinglog.vehicle.data.DatabaseDriverFactory
import platform.UIKit.UIViewController

private val appGraph: AppGraph by lazy { createAppGraph(DatabaseDriverFactory().createDriver(), SystemDeviceLocale()) }

fun MainViewController(): UIViewController = ComposeUIViewController { App(appGraph) }
