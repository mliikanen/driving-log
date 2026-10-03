package com.mikonoma.drivinglog.vehicle.picture

import com.mikonoma.drivinglog.vehicle.FakeVehicleRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PictureSweepTest {

    private val repository = FakeVehicleRepository()
    private val pictures = FakePictureStore()

    @Test
    fun theFilesOfPicturesNoVehicleUsesAreDeletedAndTheOthersKept() = runTest {
        val used = pictures.addPicture()
        val stray = pictures.addPicture()
        repository.seedVehicle("v1", "Family car", pictureId = used)
        repository.seedVehicle("v2", "Van")

        sweepPictures(repository, pictures)

        assertEquals(setOf(used), pictures.pictures.keys)
        assertEquals(listOf(setOf(used)), pictures.sweeps)
        assertEquals(false, stray in pictures.pictures)
    }

    @Test
    fun everyPictureOfEveryVehicleIsKept() = runTest {
        val a = pictures.addPicture()
        val b = pictures.addPicture()
        repository.seedVehicle("v1", "A", pictureId = a)
        repository.seedVehicle("v2", "B", pictureId = b)

        sweepPictures(repository, pictures)

        assertEquals(setOf(a, b), pictures.pictures.keys)
    }

    @Test
    fun withNoVehiclesEveryPictureIsAStray() = runTest {
        pictures.addPicture()

        sweepPictures(repository, pictures)

        assertEquals(emptySet(), pictures.pictures.keys)
    }
}
