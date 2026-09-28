package com.mikonoma.drivinglog.vehicle.ocr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage

/** iOS has no live scanner: the scan action is hidden there (`add-live-scanner`), so this is never asked. */
@Composable
actual fun rememberCameraPermission(): CameraPermission = CameraPermission(granted = false, request = { it(false) }, openSettings = {})

/** iOS has no live scanner (see [rememberCameraPermission]). */
@Composable
actual fun LiveCameraPreview(onFrame: suspend (RgbImage) -> Unit, modifier: Modifier) = Unit
