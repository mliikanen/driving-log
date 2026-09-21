package com.mikonoma.drivinglog.ui

import androidx.compose.ui.unit.dp

/**
 * Every screen is a `Scaffold` with `contentWindowInsets = WindowInsets.safeDrawing`: the system bars (the status bar, and the gesture
 * bar or the three-button navigation bar), the display cutout and the keyboard. One mechanism, so nothing sits under a bar or the
 * keyboard and the keyboard's inset is never added on top of the navigation bar's. Scrolling content also ends with [ScreenBottomSpace].
 */
val ScreenBottomSpace = 24.dp
