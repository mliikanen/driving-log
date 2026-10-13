package com.mikonoma.drivinglog.vehicle.picture

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The system's chooser: one `createChooser` over "get an image" whose extra initial intents are the camera apps, so the system lists
 * every app that can provide an image (photo and file apps, other apps that offer images) and each camera app, and the user picks the
 * one. Choosing a photo or file app needs no permission. Choosing a camera app asks for the camera permission first when the app doesn't
 * hold it ([CameraRefinementReceiver], `ask-camera-for-camera-app`); a refusal comes back as [PhotoResult.CameraDenied].
 */
@Composable
// A failure here is "the photo can't be read" (PhotoResult.Unreadable, which the screen reports); its cause has nothing more for the
// user, and the app keeps no log to put it in.
@Suppress("SwallowedException")
actual fun rememberPhotoPicker(onResult: (PhotoResult) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onResult)
    // The file the camera is told to write to, kept across a rotation while the chooser or the camera is open.
    var cameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val cameraFile = cameraFilePath?.let(::File)
        cameraFilePath = null
        scope.launch { latest(withContext(ioDispatcher) { photoOf(context, result, cameraFile) }) }
    }
    return remember(launcher) {
        PhotoPicker {
            val cameraFile = newCameraFile(context)
            cameraFilePath = cameraFile.path
            try {
                launcher.launch(chooser(context, cameraFile))
            } catch (e: ActivityNotFoundException) {
                // No app at all can provide an image.
                cameraFilePath = null
                latest(PhotoResult.Unreadable)
            }
        }
    }
}

private fun newCameraFile(context: Context): File {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    directory.listFiles()?.forEach { it.delete() } // a photo that was never read (the app was killed) is not kept
    return File(directory, "capture-${System.nanoTime()}.jpg")
}

private fun chooser(context: Context, cameraFile: File): Intent {
    val cameraUri = cameraUri(context, cameraFile)
    val pick = Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
    return Intent.createChooser(pick, "Add picture")
        .putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraApps(context, cameraUri))
        .putExtra(Intent.EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER, cameraRefinement(context))
}

/**
 * One chooser entry per camera app, each aimed at that app, so the chooser shows every camera app under its own name and icon. The camera
 * apps are visible to the app through the manifest's `<queries>` entry for `ACTION_IMAGE_CAPTURE`.
 */
// The <queries> entry is in the app's manifest (androidApp), which lint of this library module does not see.
@SuppressLint("QueryPermissionsNeeded")
private fun cameraApps(context: Context, output: Uri): Array<Intent> =
    context.packageManager.queryIntentActivities(captureIntent(output), PackageManager.MATCH_DEFAULT_ONLY)
        .map { captureIntent(output).setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
        .toTypedArray()

private fun cameraUri(context: Context, cameraFile: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.pictures", cameraFile)

/** An image the user chose (a Uri) or took (the file the camera wrote), or [PhotoResult.Cancelled] when there is none. */
// A failure here is "the photo can't be read" (PhotoResult.Unreadable, which the screen reports); its cause has nothing more for the
// user, and the app keeps no log to put it in.
@Suppress("SwallowedException")
private fun photoOf(context: Context, result: ActivityResult, cameraFile: File?): PhotoResult {
    if (result.resultCode != Activity.RESULT_OK) {
        // Only a refused camera permission is CameraDenied; leaving the chooser, the camera or any app is a cancel.
        val refused = cameraFile != null && consumeDenial(cameraUri(context, cameraFile))
        cameraFile?.delete()
        return if (refused) PhotoResult.CameraDenied else PhotoResult.Cancelled
    }
    val data = result.data
    val uri = data?.data ?: data?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    return try {
        when {
            uri != null -> readUri(context, uri).also { cameraFile?.delete() }
            cameraFile != null && cameraFile.length() > 0 -> PhotoResult.Chosen(cameraFile.readBytes()).also { cameraFile.delete() }
            else -> PhotoResult.Unreadable
        }
    } catch (e: IOException) {
        PhotoResult.Unreadable
    } catch (e: SecurityException) {
        PhotoResult.Unreadable
    }
}

/**
 * The photo's bytes, read at once because the provider's grant on the Uri does not last. At most one byte more than
 * [MAX_PHOTO_BYTES] is read, which is enough for the picture logic to refuse it.
 */
private fun readUri(context: Context, uri: Uri): PhotoResult {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readNBytes((MAX_PHOTO_BYTES + 1).toInt()) }
    return if (bytes == null || bytes.isEmpty()) PhotoResult.Unreadable else PhotoResult.Chosen(bytes)
}
