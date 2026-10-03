package com.mikonoma.drivinglog.vehicle.picture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.headerTextButtonColors
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
 * * and buttons zoom in steps (the keyboard also moves it), turn the photo a quarter turn clockwise and reset the frame; the frame never leaves the
 * photo (the [CropState] keeps it inside). The app bar's "Use photo" confirms the crop, with the turns it was made at, and back navigation (its arrow or
 * the system's) discards the photo; nothing else leaves the screen, so a photo cannot be used without being cropped. The frame and the turns survive a rotation of the device.
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
    // The free space the controls leave, in window coordinates: the frame is centred in it. The photo itself is drawn over the whole window.
    var freeSpace by remember { mutableStateOf<Rect?>(null) }
    val latestCrop by rememberUpdatedState(crop)
    val latestSpace by rememberUpdatedState(freeSpace)
    val margin = with(LocalDensity.current) { 24.dp.toPx() }

    /** Where the frame is drawn on a screen of [width] by [height]: in the free space (the whole screen until the controls have been measured). */
    val frameFor: (Float, Float) -> CropFrame = { width, height ->
        val space = latestSpace
        if (space == null) cropFrame(0f, 0f, width, height, margin) else cropFrame(space.left, space.top, space.right, space.bottom, margin)
    }

    // Two layers: the canvas fills the whole window (its black background and the photo reach every edge, behind the system bars), and above it a Scaffold
    // like every screen's: the app bar in the app's theme (back navigation cancels the crop, "Use photo" confirms it) and the buttons, kept clear of the
    // system bars and display cutouts by the safe drawing insets.
    // The gestures are detected here, on the layer that holds both: the Scaffold's surface takes the touches that reach it before the canvas below could, but a parent still
    // sees the ones no child consumed (a button consumes its own tap). The frame a gesture is applied to is read at each event, so a button's change is never lost.
    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(bitmap) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                val frame = frameFor(size.width.toFloat(), size.height.toFloat())
                update(latestCrop.transformedBy(frame, centroid.x, centroid.y, pan.x, pan.y, zoom))
            }
        },
    ) {
        CropCanvas(Modifier.fillMaxSize(), bitmap, crop, frameColor, focus, controls, frameFor)
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { CropTopBar(onCancel = onCancel, onUse = { onConfirm(crop.rect(), view.quarterTurns) }) },
        ) { padding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                val space = Modifier.onGloballyPositioned { freeSpace = it.boundsInRoot() }
                if (maxWidth > maxHeight) {
                    Row(Modifier.fillMaxSize()) {
                        Spacer(space.weight(1f).fillMaxSize())
                        CropButtons(controls, sideBySide = true)
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Spacer(space.weight(1f).fillMaxWidth())
                        CropButtons(controls, sideBySide = false)
                    }
                }
            }
        }
    }
}

/** What the buttons and the keyboard do, so the two ways share one definition. */
private class CropControls(val zoomIn: () -> Unit, val zoomOut: () -> Unit, val move: (CropMove) -> Unit, val rotate: () -> Unit, val reset: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropTopBar(onCancel: () -> Unit, onUse: () -> Unit) {
    Column {
        TopAppBar(
            colors = drivingLogTopAppBarColors(),
            title = { Text("Crop the photo") },
            navigationIcon = { BackButton(onCancel) },
            actions = {
                TextButton(
                    colors = headerTextButtonColors(),
                    onClick = onUse,
                    modifier = Modifier.testTag("use_photo"),
                ) { Text("Use photo") }
            },
        )
        HeaderDivider()
    }
}

@Composable
private fun CropCanvas(
    modifier: Modifier,
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    crop: CropState,
    frameColor: Color,
    focus: FocusRequester,
    controls: CropControls,
    frameFor: (Float, Float) -> CropFrame,
) {
    Canvas(
        modifier.testTag("crop_frame")
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
            },
    ) {
        val frame = frameFor(size.width, size.height)
        val left = frame.left
        val top = frame.top
        val side = frame.side
        val scale = side / crop.side // screen pixels per pixel of the photo
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
        drawRect(scrim, Offset(0f, top + side), Size(size.width, size.height - top - side))
        drawRect(scrim, Offset(0f, top), Size(left, side))
        drawRect(scrim, Offset(left + side, top), Size(size.width - left - side, side))
        drawRect(frameColor, Offset(left, top), Size(side, side), style = Stroke(width = 3.dp.toPx()))
    }
}

/** The buttons: one row under the photo, or one column beside it, each named for a screen reader and acting once per tap. (The photo is moved by dragging.) */
@Composable
private fun CropButtons(controls: CropControls, sideBySide: Boolean) {
    val zoomOut = @Composable { CropButton("Zoom out", "\u2212", controls.zoomOut) }
    val zoomIn = @Composable { CropButton("Zoom in", "+", controls.zoomIn) }
    val rest = @Composable {
        CropButton("Rotate photo", "\u21BB", controls.rotate)
        TextButton(onClick = controls.reset) { Text("Reset", color = Color.White) }
    }
    if (sideBySide) {
        // A column: "+" above "-", as on a slider that runs upwards.
        Column(Modifier.padding(end = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            zoomIn()
            zoomOut()
            rest()
        }
    } else {
        // A row: "-" to the left of "+", as on a slider that runs to the right.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            zoomOut()
            zoomIn()
            rest()
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
