package com.mikonoma.drivinglog.di

import app.cash.sqldelight.db.SqlDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.vehicle.data.SqlDelightVehicleRepository
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import com.mikonoma.drivinglog.vehicle.add.AddVehicleProcessor
import com.mikonoma.drivinglog.vehicle.details.VehicleDetailsProcessor
import com.mikonoma.drivinglog.vehicle.distance.LogDistanceProcessor
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.SystemDeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleProcessor
import com.mikonoma.drivinglog.vehicle.list.VehicleListProcessor
import com.mikonoma.drivinglog.vehicle.picture.FileVehiclePictureStore
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.VehiclePictureStore
import com.mikonoma.drivinglog.vehicle.log.VehicleLogProcessor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.io.files.Path

@DependencyGraph(AppScope::class)
interface AppGraph {
    val deviceLocale: DeviceLocale
    val deviceTimeZone: DeviceTimeZone
    val vehicleRepository: VehicleRepository
    val vehiclePictureStore: VehiclePictureStore

    val vehicleListProcessor: VehicleListProcessor
    val addVehicleProcessor: AddVehicleProcessor
    val vehicleDetailsProcessorFactory: VehicleDetailsProcessor.Factory
    val editVehicleProcessorFactory: EditVehicleProcessor.Factory
    val vehicleLogProcessorFactory: VehicleLogProcessor.Factory
    val logDistanceProcessorFactory: LogDistanceProcessor.Factory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides driver: SqlDriver, @Provides deviceLocale: DeviceLocale, @Provides picturesRoot: Path, @Provides imageCodec: ImageCodec): AppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(driver: SqlDriver): DrivingLogDatabase = DrivingLogDatabase(driver)

    @Provides
    fun provideClock(): Clock = Clock.System

    @Provides
    fun provideDeviceTimeZone(): DeviceTimeZone = SystemDeviceTimeZone()

    @OptIn(ExperimentalUuidApi::class)
    @Provides
    @SingleIn(AppScope::class)
    fun provideVehiclePictureStore(picturesRoot: Path, clock: Clock): VehiclePictureStore =
        FileVehiclePictureStore(picturesRoot, ioDispatcher, clock) { Uuid.random().toString() }

    @OptIn(ExperimentalUuidApi::class)
    @Provides
    @SingleIn(AppScope::class)
    fun provideVehicleRepository(
        database: DrivingLogDatabase,
        clock: Clock,
        deviceTimeZone: DeviceTimeZone,
        pictures: VehiclePictureStore,
    ): VehicleRepository =
        SqlDelightVehicleRepository(
            database = database,
            clock = clock,
            newId = { Uuid.random().toString() },
            dispatcher = ioDispatcher,
            deviceTimeZone = deviceTimeZone,
            pictures = pictures,
        )
}

// Metro only rewrites createGraphFactory() in modules with its plugin applied, so the platform shells call this.
fun createAppGraph(driver: SqlDriver, deviceLocale: DeviceLocale, picturesRoot: Path, imageCodec: ImageCodec): AppGraph =
    createGraphFactory<AppGraph.Factory>().create(driver, deviceLocale, picturesRoot, imageCodec)
