package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.runtime.Composable

/** Opens the system photo picker. */
class PhotoPicker(val launch: () -> Unit)

/**
 * The system photo picker (the Android Photo Picker, `PHPicker` on iOS), which needs no permission to read the photo library.
 * [onResult] gets the chosen photo's bytes; null when the user left the picker without choosing; and an empty array when the
 * chosen photo could not be read, which the picture logic reports as "could not be opened".
 */
@Composable
expect fun rememberPhotoPicker(onResult: (ByteArray?) -> Unit): PhotoPicker
