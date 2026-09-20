package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.serialization.Serializable

/** A picture the user has cropped and confirmed, waiting in the pending area until the vehicle is saved. */
@Serializable
data class PendingPicture(val pendingId: String)

/** What an edit does to a vehicle's picture. */
sealed interface PictureChange {
    /** The vehicle keeps the picture it has (or none). */
    data object Keep : PictureChange

    /** The vehicle has no picture after the edit. */
    data object Remove : PictureChange

    /** The vehicle's picture becomes [picture]. */
    data class Replace(val picture: PendingPicture) : PictureChange
}
