package com.mikonoma.drivinglog.vehicle.distance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.mikonoma.drivinglog.ui.BackButton
import com.mikonoma.drivinglog.ui.PhotoIcons
import com.mikonoma.drivinglog.ui.theme.HeaderDivider
import com.mikonoma.drivinglog.ui.theme.drivingLogTopAppBarColors
import com.mikonoma.drivinglog.ui.theme.headerTextButtonColors
import com.mikonoma.drivinglog.vehicle.picture.EventPhotoDraft
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult
import com.mikonoma.drivinglog.vehicle.picture.PictureError
import com.mikonoma.drivinglog.vehicle.picture.rememberPhotoPicker

/**
 * The optional note (`add-event-notes`, restyled by `restyle-note-field`): a Material 3 outlined field — the same
 * visual family as [KindSelector]/[VehicleSelector] — with a "Note" label, built from
 * [OutlinedTextFieldDefaults.DecorationBox] rather than a real [OutlinedTextField]: a real field's `value` has no
 * ellipsis support (only clipping), and the two-line end-ellipsized preview is an existing, unchanged requirement.
 * [innerTextField] is a plain [Text] (the prompt or the truncated note), never a real text field, so there is no
 * cursor, focus or keyboard to trigger — tapping only ever opens the full-screen editor. The `value` passed to the
 * decoration box is the same text that is shown, always non-empty, purely to keep the "Note" label minimized and
 * floated (the box never receives real focus, so an empty, never-focused field would otherwise show a large,
 * centered label with no visible content). `DecorationBox` has no `modifier` of its own — in the usual pattern it
 * decorates a `BasicTextField`, which carries one — so the tap target, width and test tag are on the wrapping [Box].
 *
 * `internal`, not `private` (add-event-editing): reused as-is by the event details screen's "Edit" action, where
 * [showRemoveAction] is false — that screen's only way to clear a note is opening the editor and blanking the text
 * (see its own spec scenario), not a separate trash-can action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteField(pendingNote: String?, onOpen: () -> Unit, onRemove: () -> Unit, showRemoveAction: Boolean = true) {
    val shown = pendingNote ?: "Add a note..."
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Add a note", role = Role.Button, onClick = onOpen)
            .testTag("log_note")
            // The "Note" label and the shown text are separate child nodes of the DecorationBox (there is no real
            // OutlinedTextField merging them onto this element's own semantics the way it normally would); adding
            // (not clearing) a contentDescription is what makes this element's own accessible text/label include
            // the shown text, without touching the trailing icon's own, separate semantics below.
            .semantics { contentDescription = shown },
        // DecorationBox has no modifier of its own (see the class doc); propagating this Box's min
        // constraints down is what makes it actually stretch to fillMaxWidth instead of wrapping its content.
        propagateMinConstraints = true,
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = shown,
            innerTextField = {
                Text(
                    text = shown,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (pendingNote != null) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            },
            enabled = true,
            singleLine = false,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = { Text("Note") },
            trailingIcon = if (pendingNote != null && showRemoveAction) {
                {
                    IconButton(onClick = onRemove, modifier = Modifier.testTag("log_note_remove")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove note")
                    }
                }
            } else {
                null
            },
        )
    }
}

/**
 * Up to 5 photos attached to the entry (`add-event-pictures`): a thumbnail per attached photo, each with its own
 * remove action, and an "Add photo" tile while under the cap. Tapping "Add photo" opens the system's own chooser of
 * image sources, and the chosen photo appears in the strip with no crop step — an event photo is looked at for its
 * content, not shown as a small square avatar the way a vehicle's own picture is.
 */
@Composable
internal fun PhotoStripField(
    photos: EventPhotoDraft,
    previewUris: List<Pair<String, String>>,
    onPhotoPicked: (PhotoResult) -> Unit,
    onRemoveRequested: (String) -> Unit,
) = PhotoStripField(
    isFull = photos.isFull,
    error = photos.error,
    previewUris = previewUris,
    onPhotoPicked = onPhotoPicked,
    onRemoveRequested = onRemoveRequested,
)

/**
 * The strip itself, decoupled from [EventPhotoDraft]: `internal`, not `private` (add-event-editing), reused as-is by
 * the event details screen's "Edit" action, whose photos come from two sources (already-saved and newly picked)
 * rather than one [EventPhotoDraft] — the caller merges [previewUris] and computes [isFull]/[error] from whichever
 * source(s) apply, and decides which removal path [onRemoveRequested]'s id means.
 */
@Composable
internal fun PhotoStripField(
    isFull: Boolean,
    error: PictureError?,
    previewUris: List<Pair<String, String>>,
    onPhotoPicked: (PhotoResult) -> Unit,
    onRemoveRequested: (String) -> Unit,
) {
    val picker = rememberPhotoPicker(onPhotoPicked)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Photos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((id, uri) in previewUris) {
                PhotoThumbnail(uri, id, onRemove = { onRemoveRequested(id) }, modifier = Modifier.testTag("event_photo_$id"))
            }
            if (!isFull) {
                Box(
                    Modifier
                        .size(72.dp)
                        .testTag("add_photo")
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClickLabel = "Add photo", role = Role.Button, onClick = picker.launch),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PhotoIcons.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        error?.let {
            Text(
                when (it) {
                    PictureError.COULD_NOT_OPEN -> "The photo could not be opened"
                    PictureError.CAMERA_DENIED -> "Camera access is turned off. You can allow it in the device settings."
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("photo_error"),
            )
        }
    }
}

@Composable
internal fun PhotoThumbnail(uri: String, pendingId: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(72.dp)) {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            loading = {},
            error = {},
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                .clickable(onClickLabel = "Remove photo", role = Role.Button, onClick = onRemove)
                .testTag("photo_remove_$pendingId"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * The full-screen note editor: a plain multi-line text field, seeded with the note pending so far. It replaces the log event
 * form's own content in the same window while open — not a separate [Dialog] (a second Android window): once that window's
 * one text field had taken and released IME focus, the accessibility tree stopped exposing its content at all, confirmed with
 * plain `adb shell uiautomator dump`, independent of any test tooling. Every other text field in the app already lives in an
 * ordinary single-window screen and has never shown this. Back navigation — the toolbar's arrow or the system's own
 * gesture/button, both wired to [onAttach] — attaches what was typed; "Discard" is the one way to leave without attaching it.
 * Not its own navigation destination: there is nothing to restore it into once the form itself is gone.
 *
 * `internal`, not `private` (add-event-editing): reused as-is by the event details screen's "Edit" action, in a
 * mode where [onAttach] saves directly to the stored event instead of attaching to an in-memory pending draft —
 * the only difference, per this requirement's own design.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal fun NoteEditorContent(text: String, onTextChanged: (String) -> Unit, onAttach: () -> Unit, onDiscard: () -> Unit) {
    BackHandler(onBack = onAttach)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = drivingLogTopAppBarColors(),
                    title = { Text("Note") },
                    navigationIcon = { BackButton(onAttach) },
                    actions = {
                        TextButton(
                            colors = headerTextButtonColors(),
                            onClick = onDiscard,
                            modifier = Modifier.testTag("note_editor_discard"),
                        ) { Text("Discard") }
                    },
                )
                HeaderDivider()
            }
        },
    ) { padding ->
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        // Seeded once per time the editor opens (this composable is only in composition while it is open, so a
        // fresh `rememberTextFieldState` runs each time). Its own default `initialSelection` places the cursor at
        // the end of any existing text — not position 0, `TextFieldValue`'s default for a freshly focused field,
        // which left an edit of a non-empty note (add-event-editing) inserting new text before the old instead of
        // replacing it. `TextFieldValue` itself (even with its default, zero selection) was tried first and
        // rejected: on this Compose Multiplatform version, an `OutlinedTextField` bound to it froze the whole
        // screen's touch and back handling after any edit, reproduced independent of the selection value — a
        // library-level interaction bug, not this screen's own state. `TextFieldState` doesn't share it. Not
        // re-seeded on every keystroke: after this, the field's own edits are the source of truth, fed back up via
        // onTextChanged.
        val fieldState = rememberTextFieldState(initialText = text)
        LaunchedEffect(fieldState) { snapshotFlow { fieldState.text.toString() }.collect(onTextChanged) }
        OutlinedTextField(
            state = fieldState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .focusRequester(focus)
                .testTag("note_editor_field"),
        )
    }
}
