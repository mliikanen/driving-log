package com.mikonoma.drivinglog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** "A photo can't be taken": the missing-permission message's text for a camera refused when taking a photo. */
const val CAMERA_PHOTO_MISSING = "A photo can't be taken because camera access is turned off."

/**
 * The one message for something the user asked for that can't be done because a permission is missing (`app-shell`, "A missing permission
 * is explained with a way to the settings"): [text] says what can't be done and which access is turned off, and "Open settings" opens the
 * app's own page in the device settings. Shown only for a missing permission, never for another failure or for leaving a chooser
 * without a result. [onAllowed] is called when the user is back in the app with camera access allowed, so the screen can drop the
 * message.
 */
@Composable
fun PermissionMissingMessage(text: String, onAllowed: () -> Unit, modifier: Modifier = Modifier) {
    val openSettings = rememberOpenAppSettings()
    OnCameraAllowed(onAllowed)
    Column(
        modifier.testTag("permission_missing"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = openSettings, modifier = Modifier.testTag("permission_open_settings")) { Text("Open settings") }
    }
}

/** Opens the app's own page in the device settings, where its permissions are controlled. */
@Composable
expect fun rememberOpenAppSettings(): () -> Unit

/** Calls [onAllowed] whenever the user is back in the app (from the device settings, say) and camera access is allowed. */
@Composable
expect fun OnCameraAllowed(onAllowed: () -> Unit)
