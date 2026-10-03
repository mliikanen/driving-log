package com.mikonoma.drivinglog.vehicle.picture

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
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
 * The system's chooser: one `createChooser` over "get an image" whose extra initial intent is the camera app, so the system lists
 * every app that can provide an image (photo and file apps, other apps that offer images) and the camera, and the user picks the
 * one. The app declares no permission: the camera app takes the picture through the intent and holds the camera permission itself.
 */
@Composable
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
    val cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.pictures", cameraFile)
    val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        .putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .apply { clipData = ClipData.newRawUri("", cameraUri) } // carries the grant to the camera app through the chooser
    val pick = Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
    return Intent.createChooser(pick, "Add picture").putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
}

/** An image the user chose (a Uri) or took (the file the camera wrote), or [PhotoResult.Cancelled] when there is none. */
private fun photoOf(context: Context, result: ActivityResult, cameraFile: File?): PhotoResult {
    if (result.resultCode != Activity.RESULT_OK) {
        cameraFile?.delete()
        return PhotoResult.Cancelled
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
