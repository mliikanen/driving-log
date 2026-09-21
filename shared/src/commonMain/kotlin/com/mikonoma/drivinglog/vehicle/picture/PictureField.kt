package com.mikonoma.drivinglog.vehicle.picture

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.domain.VehicleType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mikonoma.drivinglog.ui.PhotoIcons
import com.mikonoma.drivinglog.ui.VehiclePicture

/**
 * The picture part of the add and edit forms: the preview, which is itself the action ("Add picture" or "Change picture": tapping
 * it opens the system chooser of where the photo comes from), "Remove picture", the message
 * for a photo that could not be opened, and the full-screen crop while one is open. What it shows comes from [picture], the
 * [previewUri] and the [cropImage] of the form's state, and every action is one of the callbacks, so both forms use it the same way.
 */
@Composable
fun PictureField(
    picture: PictureEditState,
    previewUri: String?,
    /** The type of the vehicle: its icon is the preview until there is a picture. Null on the add form before one is chosen. */
    type: VehicleType?,
    /** The vehicle's color, already animated by the screen: the preview's icon is drawn from it. */
    color: Rgb,
    cropImage: DecodedImage?,
    onPhotoPicked: (PhotoResult) -> Unit,
    onCropConfirmed: (CropRect, Int) -> Unit,
    onCropCancelled: () -> Unit,
    onRemove: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = rememberPhotoPicker(onPhotoPicked)
    val hasPicture = previewUri != null

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // The picture itself is the action: tapping it opens the system chooser. The small camera badge says so.
            val label = if (hasPicture) "Change picture" else "Add picture"
            Box(
                Modifier
                    .size(96.dp)
                    .testTag("picture_preview")
                    .semantics { contentDescription = label }
                    .clickable(onClickLabel = label, role = Role.Button, onClick = picker.launch),
            ) {
                VehiclePicture(previewUri, type, color, Modifier.fillMaxSize(), placeholderDescription = null)
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(4.dp).size(28.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary).testTag("picture_badge"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PhotoIcons.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            if (hasPicture) {
                TextButton(onClick = onRemove, modifier = Modifier.testTag("remove_picture")) { Text("Remove picture") }
            }
        }
        picture.error?.let { error ->
            Text(
                when (error) {
                    PictureError.COULD_NOT_OPEN -> "The picture could not be opened"
                    PictureError.CAMERA_DENIED -> "Camera access is turned off. You can allow it in the device settings."
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("picture_error"),
            )
        }
    }

    if (picture.isCropping) {
        // A restored form has the photo's name but not its decoded pixels: ask for them once.
        LaunchedEffect(picture.cropSourceId, cropImage == null) { if (cropImage == null) onRefresh() }
        Dialog(onDismissRequest = onCropCancelled, properties = cropDialogProperties()) {
            if (cropImage != null) {
                CropScreen(cropImage, onConfirm = onCropConfirmed, onCancel = onCropCancelled)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}
