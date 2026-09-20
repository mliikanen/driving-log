package com.mikonoma.drivinglog

import androidx.compose.runtime.Composable
import com.mikonoma.drivinglog.home.HomeProcessor
import com.mikonoma.drivinglog.home.HomeScreen
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme

@Composable
fun App(homeProcessor: HomeProcessor) {
    DrivingLogTheme {
        HomeScreen(homeProcessor)
    }
}
