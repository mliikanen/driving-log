package com.mikonoma.drivinglog.vehicle.picture

import com.mikonoma.drivinglog.ui.theme.CoolPlatinum
import com.mikonoma.drivinglog.ui.theme.AsphaltText
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The crop screen: the photo under a fixed square frame. Dragging moves the photo, pinching zooms it, and the frame never leaves
 * the photo (the [CropState] keeps it inside). "Use photo" confirms the crop, "Cancel" discards the photo; nothing else leaves
 * the screen, so a photo cannot be used without being cropped.
 */
@Composable
fun CropScreen(image: DecodedImage, onConfirm: (CropRect) -> Unit, onCancel: () -> Unit) {
    var crop by remember(image) { mutableStateOf(CropState.initial(image.width, image.height)) }
    val bitmap = remember(image) { image.toImageBitmap() }
    // The crop screen is a fixed black surface, so its frame is white in both schemes: the primary color is nearly invisible on black in light mode.
    val frameColor = Color.White

    Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        Text(
            "Crop the photo",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(16.dp),
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val margin = with(LocalDensity.current) { 24.dp.toPx() }
            val frame = min(constraints.maxWidth, constraints.maxHeight) - 2 * margin
            Canvas(
                Modifier.fillMaxSize().testTag("crop_frame").pointerInput(image, frame) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val left = (size.width - frame) / 2f
                        val top = (size.height - frame) / 2f
                        val scale = frame / crop.side // screen pixels per pixel of the photo
                        // The photo point under the fingers stays under them while zooming.
                        val focusX = crop.centerX - crop.side / 2 + (centroid.x - left) / scale
                        val focusY = crop.centerY - crop.side / 2 + (centroid.y - top) / scale
                        crop = crop.panBy(-pan.x / scale, -pan.y / scale).zoomBy(zoom, focusX, focusY)
                    }
                },
            ) {
                val left = (size.width - frame) / 2f
                val top = (size.height - frame) / 2f
                val rect = crop.rect()
                drawImage(
                    image = bitmap,
                    srcOffset = IntOffset(rect.x, rect.y),
                    srcSize = IntSize(rect.side, rect.side),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(frame.roundToInt(), frame.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
                drawRect(frameColor, Offset(left, top), Size(frame, frame), style = Stroke(width = 3.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            TextButton(onClick = onCancel) { Text("Cancel", color = Color.White) }
            // Fixed colors like the rest of this screen: the theme's primary is Oil Slick Blue in light mode, which is lost on black.
            Button(
                onClick = { onConfirm(crop.rect()) },
                colors = ButtonDefaults.buttonColors(containerColor = CoolPlatinum, contentColor = AsphaltText),
            ) { Text("Use photo") }
        }
    }
}
