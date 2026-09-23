package com.mikonoma.drivinglog.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

/**
 * A full-screen dialog's dismiss action (close-icon-for-forms): the log event form and the add/edit vehicle forms,
 * where leaving always discards whatever was entered, per Material's own distinction between "back" (return to
 * where you were) and "close" (leave without keeping this). Same `back` test tag as [BackButton] — the tag names
 * the action every Maestro flow already taps by it, not the icon.
 */
@Composable
fun CloseButton(onClose: () -> Unit) {
    IconButton(onClick = onClose, modifier = Modifier.testTag("back")) {
        Icon(Icons.Filled.Close, contentDescription = "Close")
    }
}

/** Explains the "*" convention on a required field's label (emphasize-required-fields), once per form. */
@Composable
fun RequiredFieldNote() {
    Text(
        "* indicates a required field",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
