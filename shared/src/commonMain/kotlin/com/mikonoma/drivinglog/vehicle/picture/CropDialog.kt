package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.ui.window.DialogProperties

/**
 * The properties of the dialog the crop screen is shown in: as wide as the screen and, where the platform lays a dialog out inside the system bars (Android),
 * edge to edge, so the crop screen's black background and its photo reach every edge. The screen keeps its buttons clear of the bars itself.
 */
expect fun cropDialogProperties(): DialogProperties
