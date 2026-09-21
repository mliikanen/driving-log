package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikonoma.drivinglog.ui.theme.AsphaltText
import com.mikonoma.drivinglog.ui.theme.CoolPlatinum
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How dark the scrim over the part of the photo outside the frame is. */
private const val SCRIM_ALPHA = 0.6f

/** What the crop screen keeps across a rotation of the device and the restart of the process: the turns of the photo and the frame (see [CropState.toSaved]). */
private data class CropView(val quarterTurns: Int, val crop: List<Float>?)

private val CropViewSaver = listSaver<CropView, Any>(
    save = { listOf(it.quarterTurns) + (it.crop ?: emptyList()) },
    restore = { saved -> CropView(saved[0] as Int, saved.drop(1).map { it as Float }.ifEmpty { null }) },
)

/**
 * The crop screen: the photo under a fixed square frame, the part of it outside the frame dimmed. Dragging moves the photo, pinching zooms it,
 * and buttons (or the keyboard) do the same in steps, turn the photo a quarter turn clockwise and reset the frame; the frame never leaves the
 * photo (the [CropState] keeps it inside). "Use photo" confirms the crop, with the turns it was made at, and "Cancel" discards the photo;
 * nothing else leaves the screen, so a photo cannot be used without being cropped. The frame and the turns survive a rotation of the device.
 */
@Composable
fun CropScreen(image: DecodedImage, onConfirm: (CropRect, Int) -> Unit, onCancel: () -> Unit) {
    var view by rememberSaveable(stateSaver = CropViewSaver) { mutableStateOf(CropView(0, null)) }
    // The photo as turned; the frame is in its pixels. A frame saved for a photo of another size means nothing and is not used.
    val shown = remember(image, view.quarterTurns) { image.turnedClockwise(view.quarterTurns) }
    val bitmap = remember(shown) { shown.toImageBitmap() }
    var crop by remember(shown) {
        mutableStateOf(CropState.restore(view.crop, shown.width, shown.height) ?: CropState.initial(shown.width, shown.height))
    }
    fun update(next: CropState) {
        crop = next
        view = view.copy(crop = next.toSaved())
    }
    fun turn() {
        // The frame follows the photo: the turned state is what the screen restores for the turned photo.
        view = CropView((view.quarterTurns + 1).mod(4), crop.rotatedClockwise().toSaved())
    }
    // The crop screen is a fixed black surface, so its frame is white in both schemes: the primary color is nearly invisible on black in light mode.
    val frameColor = Color.White
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val controls = CropControls(
        zoomIn = { update(crop.zoomInStep()) },
        zoomOut = { update(crop.zoomOutStep()) },
        move = { update(crop.movePhoto(it)) },
        rotate = ::turn,
        reset = { update(CropState.initial(shown.width, shown.height)) },
    )

    Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        Text(
            "Crop the photo",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(16.dp),
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val sideBySide = maxWidth > maxHeight
            val canvas = @Composable { modifier: Modifier ->
                CropCanvas(modifier, bitmap, crop, frameColor, focus, controls, onGesture = ::update)
            }
            if (sideBySide) {
                Row(Modifier.fillMaxSize()) {
                    canvas(Modifier.weight(1f).fillMaxSize())
                    CropButtons(controls, sideBySide = true)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    canvas(Modifier.weight(1f).fillMaxWidth())
                    CropButtons(controls, sideBySide = false)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCancel) { Text("Cancel", color = Color.White) }
            // Fixed colors like the rest of this screen: the theme's primary is Oil Slick Blue in light mode, which is lost on black.
            Button(
                onClick = { onConfirm(crop.rect(), view.quarterTurns) },
                colors = ButtonDefaults.buttonColors(containerColor = CoolPlatinum, contentColor = AsphaltText),
            ) { Text("Use photo") }
        }
    }
}

/** What the buttons and the keyboard do, so the two ways share one definition. */
private class CropControls(
    val zoomIn: () -> Unit,
    val zoomOut: () -> Unit,
    val move: (CropMove) -> Unit,
    val rotate: () -> Unit,
    val reset: () -> Unit,
)

@Composable
private fun CropCanvas(
    modifier: Modifier,
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    crop: CropState,
    frameColor: Color,
    focus: FocusRequester,
    controls: CropControls,
    onGesture: (CropState) -> Unit,
) {
    val latest by rememberUpdatedState(crop)
    BoxWithConstraints(modifier) {
        val margin = with(LocalDensity.current) { 24.dp.toPx() }
        val frame = min(constraints.maxWidth, constraints.maxHeight) - 2 * margin
        Canvas(
            Modifier.fillMaxSize().testTag("crop_frame")
                .focusRequester(focus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> controls.move(CropMove.Left)
                        Key.DirectionRight -> controls.move(CropMove.Right)
                        Key.DirectionUp -> controls.move(CropMove.Up)
                        Key.DirectionDown -> controls.move(CropMove.Down)
                        Key.Plus, Key.Equals, Key.NumPadAdd -> controls.zoomIn()
                        Key.Minus, Key.NumPadSubtract -> controls.zoomOut()
                        else -> return@onKeyEvent false
                    }
                    true
                }
                .pointerInput(bitmap, frame) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // The frame as it is now: a button may have changed it since the gesture began.
                        val current = latest
                        val left = (size.width - frame) / 2f
                        val top = (size.height - frame) / 2f
                        val scale = frame / current.side // screen pixels per pixel of the photo
                        // The photo point under the fingers stays under them while zooming.
                        val focusX = current.centerX - current.side / 2 + (centroid.x - left) / scale
                        val focusY = current.centerY - current.side / 2 + (centroid.y - top) / scale
                        onGesture(current.panBy(-pan.x / scale, -pan.y / scale).zoomBy(zoom, focusX, focusY))
                    }
                },
        ) {
            val left = (size.width - frame) / 2f
            val top = (size.height - frame) / 2f
            val scale = frame / crop.side // screen pixels per pixel of the photo
            val cropLeft = crop.centerX - crop.side / 2
            val cropTop = crop.centerY - crop.side / 2
            // Only the part of the photo that is on the canvas is drawn (at a high zoom the whole photo would be many screens wide).
            val srcLeft = floor(max(0f, cropLeft - left / scale)).toInt()
            val srcTop = floor(max(0f, cropTop - top / scale)).toInt()
            val srcRight = ceil(min(crop.imageWidth.toFloat(), cropLeft + (size.width - left) / scale)).toInt()
            val srcBottom = ceil(min(crop.imageHeight.toFloat(), cropTop + (size.height - top) / scale)).toInt()
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(srcLeft, srcTop),
                srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                dstOffset = IntOffset((left + (srcLeft - cropLeft) * scale).roundToInt(), (top + (srcTop - cropTop) * scale).roundToInt()),
                dstSize = IntSize(((srcRight - srcLeft) * scale).roundToInt(), ((srcBottom - srcTop) * scale).roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
            // The scrim over everything outside the frame: what is left out is dimmed, not hidden.
            val scrim = Color.Black.copy(alpha = SCRIM_ALPHA)
            drawRect(scrim, Offset(0f, 0f), Size(size.width, top))
            drawRect(scrim, Offset(0f, top + frame), Size(size.width, size.height - top - frame))
            drawRect(scrim, Offset(0f, top), Size(left, frame))
            drawRect(scrim, Offset(left + frame, top), Size(size.width - left - frame, frame))
            drawRect(frameColor, Offset(left, top), Size(frame, frame), style = Stroke(width = 3.dp.toPx()))
        }
    }
}

/** The buttons: two rows under the photo, or two columns beside it, each named for a screen reader and acting once per tap. */
@Composable
private fun CropButtons(controls: CropControls, sideBySide: Boolean) {
    val first = listOf(
        Triple("Zoom out", "−", controls.zoomOut),
        Triple("Zoom in", "+", controls.zoomIn),
        Triple("Rotate photo", "↻", controls.rotate),
    )
    val second = listOf(
        Triple("Move left", "←", { controls.move(CropMove.Left) }),
        Triple("Move right", "→", { controls.move(CropMove.Right) }),
        Triple("Move up", "↑", { controls.move(CropMove.Up) }),
        Triple("Move down", "↓", { controls.move(CropMove.Down) }),
    )
    val reset = @Composable {
        TextButton(onClick = controls.reset) { Text("Reset", color = Color.White) }
    }
    if (sideBySide) {
        Row(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column { first.forEach { CropButton(it.first, it.second, it.third) }; reset() }
            Column { second.forEach { CropButton(it.first, it.second, it.third) } }
        }
    } else {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                first.forEach { CropButton(it.first, it.second, it.third) }
                reset()
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                second.forEach { CropButton(it.first, it.second, it.third) }
            }
        }
    }
}

@Composable
private fun CropButton(label: String, glyph: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = label },
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
    ) { Text(glyph, fontSize = 24.sp, color = Color.White) }
}
