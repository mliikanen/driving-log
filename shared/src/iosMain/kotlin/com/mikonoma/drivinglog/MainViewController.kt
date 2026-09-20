package com.mikonoma.drivinglog

import androidx.compose.ui.window.ComposeUIViewController
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.di.createAppGraph
import com.mikonoma.drivinglog.locale.SystemDeviceLocale
import com.mikonoma.drivinglog.vehicle.data.DatabaseDriverFactory
import com.mikonoma.drivinglog.vehicle.picture.IosImageCodec
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIViewController

/** Application Support is the application's private storage on iOS; no other app and no photo library sees it. */
@OptIn(ExperimentalForeignApi::class)
private fun picturesRoot(): Path {
    val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    return Path(support, "pictures")
}

private val appGraph: AppGraph by lazy {
    createAppGraph(DatabaseDriverFactory().createDriver(), SystemDeviceLocale(), picturesRoot(), IosImageCodec())
}

fun MainViewController(): UIViewController = ComposeUIViewController { App(appGraph) }
