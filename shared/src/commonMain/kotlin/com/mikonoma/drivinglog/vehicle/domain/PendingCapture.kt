package com.mikonoma.drivinglog.vehicle.domain

import com.mikonoma.drivinglog.vehicle.ocr.ScanResult
import kotlinx.serialization.Serializable

/** An accepted scan (`odometer-ocr-capture`): its photo, waiting in the capture store's pending area, and what the scan found. */
@Serializable
data class PendingCapture(val pendingId: String, val result: ScanResult)

/** A scan stored with an event: its photo's id in the capture store and what the scan found. Never shown to the user. */
data class StoredCapture(val photoId: String, val result: ScanResult)
