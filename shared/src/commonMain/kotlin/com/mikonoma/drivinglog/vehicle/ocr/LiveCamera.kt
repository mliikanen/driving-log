package com.mikonoma.drivinglog.vehicle.ocr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage

/**
 * The camera permission the live scanner needs (`add-live-scanner`): [granted] now, [request] to ask for it (the answer comes back
 * through its callback; asking again after "Don't ask again" answers at once), and [openSettings] for the app's page in the device
 * settings, where a refused permission can be allowed.
 */
class CameraPermission(val granted: Boolean, val request: (onAnswer: (Boolean) -> Unit) -> Unit, val openSettings: () -> Unit)

/** The camera permission's state, read afresh each time the composition that holds it starts (a scanner opening). */
@Composable
expect fun rememberCameraPermission(): CameraPermission

/**
 * The back camera's live preview (`PreviewView`, `FILL_CENTER`) with its frames, upright, handed to [onFrame] one at a time: while one
 * is being handled, newer frames are dropped, so [onFrame] can take as long as recognition takes. Stopped when it leaves the
 * composition. Android only: on iOS the scan action is hidden, and this shows nothing.
 */
@Composable
expect fun LiveCameraPreview(onFrame: suspend (RgbImage) -> Unit, modifier: Modifier = Modifier)
