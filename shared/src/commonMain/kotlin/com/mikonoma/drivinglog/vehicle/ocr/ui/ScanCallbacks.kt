package com.mikonoma.drivinglog.vehicle.ocr.ui

import com.mikonoma.drivinglog.vehicle.ocr.LiveReading
import com.mikonoma.drivinglog.vehicle.picture.PhotoResult

/**
 * What the scanner and the photo review ask of the form that shows them (`scan-initial-odometer`): the log event form and the
 * add-vehicle form each map these to their own intents.
 */
class ScanCallbacks(
    /** The live scanner's close action or back. */
    val onClose: () -> Unit,
    /** A reading was tapped in the live scanner. */
    val onReadingTapped: (LiveReading) -> Unit,
    /** The system chooser gave back a photo, or nothing. */
    val onPhotoPicked: (PhotoResult) -> Unit,
    /** A candidate was tapped on the photo review; the index is in the review's detections. */
    val onCandidateSelected: (Int) -> Unit,
    /** The photo review's "Use". */
    val onConfirm: () -> Unit,
    /** Back out of the photo review, or its "Leave". */
    val onCancel: () -> Unit,
    /** Before the chooser opens again, an earlier photo error is cleared. */
    val onErrorDismissed: () -> Unit,
)
