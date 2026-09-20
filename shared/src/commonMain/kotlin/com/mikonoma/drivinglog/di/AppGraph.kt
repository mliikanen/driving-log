package com.mikonoma.drivinglog.di

import app.cash.sqldelight.db.SqlDriver
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.locale.SystemDeviceLocale
import com.mikonoma.drivinglog.vehicle.data.SqlDelightVehicleRepository
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import com.mikonoma.drivinglog.vehicle.add.AddVehicleProcessor
import com.mikonoma.drivinglog.vehicle.details.VehicleDetailsProcessor
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleProcessor
import com.mikonoma.drivinglog.vehicle.list.VehicleListProcessor
import com.mikonoma.drivinglog.vehicle.log.VehicleLogProcessor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@DependencyGraph(AppScope::class)
interface AppGraph {
    val deviceLocale: DeviceLocale
    val vehicleRepository: VehicleRepository

    val vehicleListProcessor: VehicleListProcessor
    val addVehicleProcessor: AddVehicleProcessor
    val vehicleDetailsProcessorFactory: VehicleDetailsProcessor.Factory
    val editVehicleProcessorFactory: EditVehicleProcessor.Factory
    val vehicleLogProcessorFactory: VehicleLogProcessor.Factory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides driver: SqlDriver): AppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(driver: SqlDriver): DrivingLogDatabase = DrivingLogDatabase(driver)

    @Provides
    fun provideClock(): Clock = Clock.System

    @Provides
    fun provideDeviceLocale(): DeviceLocale = SystemDeviceLocale()

    @OptIn(ExperimentalUuidApi::class)
    @Provides
    @SingleIn(AppScope::class)
    fun provideVehicleRepository(database: DrivingLogDatabase, clock: Clock): VehicleRepository =
        SqlDelightVehicleRepository(
            database = database,
            clock = clock,
            newId = { Uuid.random().toString() },
            dispatcher = ioDispatcher,
        )
}

// Metro only rewrites createGraphFactory() in modules with its plugin applied, so the platform shells call this.
fun createAppGraph(driver: SqlDriver): AppGraph = createGraphFactory<AppGraph.Factory>().create(driver)
