package com.mikonoma.drivinglog.vehicle.picture

import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import kotlinx.coroutines.flow.first

/**
 * Deletes the picture files no vehicle refers to (left by an interrupted save) and stale pending files. Run once when the app
 * starts, before the user can have saved a picture of their own, so a picture that was just saved is never mistaken for a stray.
 */
suspend fun sweepPictures(vehicles: VehicleRepository, pictures: VehiclePictureStore) {
    val referenced = vehicles.observeVehicles().first().mapNotNull { it.pictureId }.toSet()
    pictures.sweep(referenced)
}
