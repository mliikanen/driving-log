package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.runtime.Composable

/** What the system's way of providing an image gave back. */
sealed interface PhotoResult {
    /** The bytes of the chosen or newly taken photo. */
    class Chosen(val bytes: ByteArray) : PhotoResult

    /** The user left the chooser, the camera or the app that was to provide the image without an image. */
    data object Cancelled : PhotoResult

    /** An image was provided but could not be read. */
    data object Unreadable : PhotoResult

    /**
     * The user chose the camera but has not allowed the app camera access: refused when asked, refused before, or the prompt left without
     * an answer. On Android the app needs it to open a camera app at all, because it declares the camera permission for the live scanner.
     */
    data object CameraDenied : PhotoResult
}

/** Opens the system's chooser of where the photo comes from. */
class PhotoPicker(val launch: () -> Unit)

/**
 * The system's way of choosing where an image comes from, so the user picks the app that provides it: the intent chooser on Android
 * (photo and file apps, the camera app, any other app that offers images) and the source sheet on iOS (Take Photo, Photo Library, Choose
 * File). The app draws no picker of its own. A permission is asked for only at the moment the user triggers an action that needs it:
 * camera access when the user chooses the camera, on both platforms. Choosing a photo or file needs none.
 */
@Composable
expect fun rememberPhotoPicker(onResult: (PhotoResult) -> Unit): PhotoPicker
