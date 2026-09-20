package com.mikonoma.drivinglog.vehicle.picture

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberPhotoPicker(onResult: (ByteArray?) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) latest(null) else scope.launch { latest(readPhoto(context, uri)) }
    }
    return remember(launcher) {
        PhotoPicker { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}

/**
 * The photo's bytes, read at once because the picker's grant on the Uri does not last. At most one byte more than
 * [MAX_PHOTO_BYTES] is read, which is enough for the picture logic to refuse it. A photo that cannot be read gives an empty array.
 */
private suspend fun readPhoto(context: Context, uri: Uri): ByteArray = withContext(ioDispatcher) {
    try {
        context.contentResolver.openInputStream(uri)?.use { it.readNBytes((MAX_PHOTO_BYTES + 1).toInt()) } ?: ByteArray(0)
    } catch (e: java.io.IOException) {
        ByteArray(0)
    } catch (e: SecurityException) {
        ByteArray(0)
    }
}
