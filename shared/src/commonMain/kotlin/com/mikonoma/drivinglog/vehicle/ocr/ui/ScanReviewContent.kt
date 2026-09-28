package com.mikonoma.drivinglog.vehicle.ocr.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.ScreenBottomSpace
import com.mikonoma.drivinglog.ui.theme.DrivingLogTheme
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.headerTextButtonColors
import com.mikonoma.drivinglog.vehicle.ocr.ReadingKind
import com.mikonoma.drivinglog.vehicle.ocr.ScanReview
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.rememberPhotoPicker

/** Shown in place of the form while a chosen photo is being recognized. */
@Composable
fun ScanProgressContent() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).testTag("scan_progress"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Reading the photo…")
        }
    }
}

/**
 * The review of one scan (`odometer-ocr-capture`): the photo with a box and an "ODO" or "TRIP" label over every candidate. Tapping a box
 * or its label selects it (the neutral outline turns the distance color); "Use" accepts the selection. Back leaves the form as it was.
 * With no candidate, it says so and offers another photo or leaving. Like the note editor, it replaces the form's content in the same
 * window while open.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ScanReviewContent(review: ScanReview, photoUri: String?, callbacks: ScanCallbacks) {
    val cancel = callbacks.onCancel
    BackHandler(onBack = cancel)
    val picker = rememberPhotoPicker(callbacks.onPhotoPicked)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text("Choose the reading") },
                    navigationIcon = { BackButton(cancel) },
                    actions = {
                        if (review.hasCandidates) {
                            TextButton(
                                colors = headerTextButtonColors(),
                                onClick = callbacks.onConfirm,
                                enabled = review.selectedIndex != null,
                                modifier = Modifier.testTag("scan_use"),
                            ) { Text("Use") }
                        }
                    },
                )
                HeaderDivider()
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + ScreenBottomSpace)
                .testTag("scan_review"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (review.hasCandidates) {
                Text("Tap the reading to use.")
            } else {
                Text("No reading was found in this photo.", modifier = Modifier.testTag("scan_no_reading"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = picker.launch, modifier = Modifier.testTag("scan_choose_another")) { Text("Choose another photo") }
                    TextButton(onClick = cancel, modifier = Modifier.testTag("scan_leave")) { Text("Leave") }
                }
            }
            ScannedPhoto(review, photoUri, callbacks.onCandidateSelected)
        }
    }
}

/** The photo at its own aspect ratio, full width, with every candidate's box and label placed over it in the photo's own pixels. */
@Composable
private fun ScannedPhoto(review: ScanReview, photoUri: String?, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(review.width.toFloat() / review.height)) {
        val scale = maxWidth / review.width
        AsyncImage(model = photoUri, contentDescription = "The scanned photo", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        review.detections.forEachIndexed { index, detection ->
            val kind = detection.kind ?: return@forEachIndexed
            val selected = index == review.selectedIndex
            val color = if (selected) DrivingLogTheme.domain.distance else MaterialTheme.colorScheme.outline
            val box = detection.box
            val label = if (kind == ReadingKind.ODOMETER) "ODO" else "TRIP"
            val select = Modifier
                .semantics { this.selected = selected }
                .clickable(onClickLabel = "Use $label ${detection.value}", role = Role.Button) { onSelect(index) }
            // The box is drawn around the number, outside it, so the digits stay readable however small the photo is shown.
            Box(
                Modifier
                    .offset(x = scale * box.left - BoxMargin, y = scale * box.top - BoxMargin)
                    .size(width = scale * box.width + BoxMargin * 2, height = scale * box.height + BoxMargin * 2)
                    .border(BorderStroke(if (selected) 3.dp else 2.dp, color), RoundedCornerShape(4.dp))
                    .then(select)
                    .testTag("scan_candidate_$index"),
            )
            // The label also says what was read: on a phone the photo is small, and this is the number that fills the field.
            Text(
                "$label ${detection.value}",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) DrivingLogTheme.domain.distance else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(x = scale * box.left - BoxMargin, y = scale * box.bottom + BoxMargin + 2.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    .border(BorderStroke(1.dp, color), RoundedCornerShape(4.dp))
                    .then(select)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("scan_candidate_label_$index"),
            )
        }
    }
}

/** How far a candidate's box is drawn outside the number. */
private val BoxMargin = 4.dp

/**
 * "Scan a reading" (`odometer-ocr-capture`), under the odometer field: asks for the camera permission when it is not granted, then
 * opens the live scanner whatever the answer (`add-live-scanner`: refused, the scanner says why and offers the photo flow).
 */
@Composable
fun ScanReadingAction(onOpen: () -> Unit) {
    val permission = com.mikonoma.drivinglog.vehicle.ocr.rememberCameraPermission()
    OutlinedButton(onClick = { permission.request { onOpen() } }, modifier = Modifier.testTag("scan_reading")) { Text("Scan a reading") }
}
