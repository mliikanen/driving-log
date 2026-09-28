package com.mikonoma.drivinglog.vehicle.ocr.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.ui.CloseButton
import com.mikonoma.drivinglog.ui.PhotoIcons
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.vehicle.ocr.FillCenter
import com.mikonoma.drivinglog.vehicle.ocr.LiveCameraPreview
import com.mikonoma.drivinglog.vehicle.ocr.LiveReading
import com.mikonoma.drivinglog.vehicle.ocr.LiveScanner
import com.mikonoma.drivinglog.vehicle.ocr.ReadingKind
import com.mikonoma.drivinglog.vehicle.ocr.rememberCameraPermission
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import com.mikonoma.drivinglog.vehicle.picture.rememberPhotoPicker

/**
 * The live scanner (`add-live-scanner`): the back camera's preview with every reading read in it boxed and labeled ("ODO 71140"); a tap
 * on one applies it. The close action and back leave it with nothing kept; the floating photo button runs the photo flow (the system
 * chooser, then the photo review, which returns here when left). Without the camera permission it says why there is no preview, and the
 * photo button still works. Like the photo review, it replaces the form's content in the same window while open.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LiveScannerContent(
    error: PictureError?,
    newLiveScanner: () -> LiveScanner,
    callbacks: ScanCallbacks,
) {
    val close = callbacks.onClose
    BackHandler(onBack = close)
    val permission = rememberCameraPermission()
    val picker = rememberPhotoPicker(callbacks.onPhotoPicked)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text("Scan a reading") },
                    navigationIcon = { CloseButton(close) },
                )
                HeaderDivider()
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { callbacks.onErrorDismissed(); picker.launch() },
                modifier = Modifier.testTag("scanner_photo"),
            ) { Icon(PhotoIcons.Camera, contentDescription = "Scan a photo instead") }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().testTag("scanner")) {
            if (permission.granted) {
                LiveView(newLiveScanner, callbacks.onReadingTapped)
            } else {
                NoCamera(onOpenSettings = permission.openSettings)
            }
            error?.let {
                Text(
                    when (it) {
                        PictureError.COULD_NOT_OPEN -> "The photo could not be opened"
                        PictureError.CAMERA_DENIED -> "Camera access is turned off. You can allow it in the device settings."
                    },
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)).padding(12.dp)
                        .testTag("scan_error"),
                )
            }
        }
    }
}

/** The preview with the tracked readings over it, and "Getting ready…" until the first frame has been read. */
@Composable
private fun LiveView(newLiveScanner: () -> LiveScanner, onTap: (LiveReading) -> Unit) {
    val scanner = remember { newLiveScanner() }
    // Written from the camera's analysis thread, which Compose's snapshot state allows; read when drawing.
    var readings by remember { mutableStateOf(emptyList<LiveReading>()) }
    var ready by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LiveCameraPreview(
            onFrame = { frame ->
                readings = scanner.analyze(frame)
                ready = true
            },
            modifier = Modifier.fillMaxSize(),
        )
        val density = LocalDensity.current
        val viewWidth = with(density) { maxWidth.toPx() }
        val viewHeight = with(density) { maxHeight.toPx() }
        for (reading in readings) {
            val frame = reading.frame.image
            val rect = FillCenter(frame.width, frame.height, viewWidth, viewHeight).map(reading.detection.box)
            LiveReadingMark(reading, rect.left, rect.top, rect.right, rect.bottom, onTap)
        }
        if (!ready) {
            Text(
                "Getting ready…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("scanner_getting_ready"),
            )
        }
    }
}

/** One tracked reading: its box, drawn outside the number, and a label with its kind and value; a tap on either applies it. */
@Composable
private fun LiveReadingMark(reading: LiveReading, left: Float, top: Float, right: Float, bottom: Float, onTap: (LiveReading) -> Unit) {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.outline
    val label = if (reading.detection.kind == ReadingKind.ODOMETER) "ODO" else "TRIP"
    val tap = Modifier.clickable(onClickLabel = "Use $label ${reading.detection.value}", role = Role.Button) { onTap(reading) }
    with(density) {
        Box(
            Modifier
                .offset(x = left.toDp() - MarkMargin, y = top.toDp() - MarkMargin)
                .size(width = (right - left).toDp() + MarkMargin * 2, height = (bottom - top).toDp() + MarkMargin * 2)
                .border(BorderStroke(2.dp, color), RoundedCornerShape(4.dp))
                .then(tap),
        )
        Text(
            "$label ${reading.detection.value}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .offset(x = left.toDp() - MarkMargin, y = bottom.toDp() + MarkMargin + 2.dp)
                .minimumInteractiveComponentSize()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                .border(BorderStroke(1.dp, color), RoundedCornerShape(4.dp))
                .then(tap)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("live_reading_${reading.id}"),
        )
    }
}

/** Without the camera permission: why there is no preview, and the way to the settings; the photo button still works. */
@Composable
private fun NoCamera(onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).testTag("scanner_no_camera"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "The live scanner needs camera access. You can allow it in the device settings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onOpenSettings, modifier = Modifier.testTag("scanner_open_settings")) { Text("Open settings") }
        Text(
            "You can still scan a photo with the button below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** How far a reading's box is drawn outside the number. */
private val MarkMargin = 4.dp
