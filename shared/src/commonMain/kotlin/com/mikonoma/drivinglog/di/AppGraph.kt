package com.mikonoma.drivinglog.di

import app.cash.sqldelight.db.SqlDriver
import com.mikonoma.drivinglog.auth.AuthRepository
import com.mikonoma.drivinglog.db.DrivingLogDatabase
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.vehicle.data.SqlDelightVehicleRepository
import com.mikonoma.drivinglog.vehicle.data.ioDispatcher
import com.mikonoma.drivinglog.vehicle.add.AddVehicleProcessor
import com.mikonoma.drivinglog.vehicle.details.VehicleDetailsProcessor
import com.mikonoma.drivinglog.vehicle.distance.LogEventProcessor
import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.SystemDeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleProcessor
import com.mikonoma.drivinglog.vehicle.eventdetails.EventDetailsProcessor
import com.mikonoma.drivinglog.landing.LandingProcessor
import com.mikonoma.drivinglog.vehicle.list.VehicleListProcessor
import com.mikonoma.drivinglog.vehicle.picture.FilePictureStore
import com.mikonoma.drivinglog.vehicle.color.ColorExtractor
import com.mikonoma.drivinglog.vehicle.color.HistogramColorExtractor
import com.mikonoma.drivinglog.vehicle.picture.ImageCodec
import com.mikonoma.drivinglog.vehicle.picture.PictureStore
import com.mikonoma.drivinglog.vehicle.log.VehicleLogProcessor
import com.mikonoma.drivinglog.vehicle.ocr.CaptureStore
import com.mikonoma.drivinglog.vehicle.ocr.FileCaptureStore
import com.mikonoma.drivinglog.vehicle.ocr.TextRecognizer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
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
    val authRepository: AuthRepository
    val vehicleRepository: VehicleRepository
    val vehiclePictureStore: PictureStore

    /** A separate store from [vehiclePictureStore], pointed at its own root (`add-event-pictures`). */
    @get:Named("event")
    val eventPictureStore: PictureStore

    /** The photos of accepted scans (`odometer-ocr-capture`), under their own root like [eventPictureStore]. */
    val captureStore: CaptureStore

    val landingProcessor: LandingProcessor
    val vehicleListProcessor: VehicleListProcessor
    val addVehicleProcessor: AddVehicleProcessor
    val vehicleDetailsProcessorFactory: VehicleDetailsProcessor.Factory
    val editVehicleProcessorFactory: EditVehicleProcessor.Factory
    val vehicleLogProcessorFactory: VehicleLogProcessor.Factory
    val logEventProcessorFactory: LogEventProcessor.Factory
    val eventDetailsProcessorFactory: EventDetailsProcessor.Factory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides driver: SqlDriver,
            @Provides deviceLocale: DeviceLocale,
            @Provides picturesRoot: Path,
            @Provides imageCodec: ImageCodec,
            @Provides textRecognizer: TextRecognizer,
            @Provides authRepository: AuthRepository,
        ): AppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(driver: SqlDriver): DrivingLogDatabase = DrivingLogDatabase(driver)

    @Provides
    fun provideClock(): Clock = Clock.System

    @Provides
    fun provideColorExtractor(): ColorExtractor = HistogramColorExtractor()

    @Provides
    fun provideDeviceTimeZone(): DeviceTimeZone = SystemDeviceTimeZone()

    @OptIn(ExperimentalUuidApi::class)
    @Provides
    @SingleIn(AppScope::class)
    fun providePictureStore(picturesRoot: Path, clock: Clock): PictureStore =
        FilePictureStore(picturesRoot, ioDispatcher, clock) { Uuid.random().toString() }

    /** A separate root (`{picturesRoot}/events`) from the vehicle pictures instance above, so an independent sweep
     * policy per kind never has to filter one shared directory by prefix (`add-event-pictures`, design.md). */
    @OptIn(ExperimentalUuidApi::class)
    @Provides
    @SingleIn(AppScope::class)
    @Named("event")
    fun provideEventPictureStore(picturesRoot: Path, clock: Clock): PictureStore =
        FilePictureStore(Path(picturesRoot, "events"), ioDispatcher, clock) { Uuid.random().toString() }

    @OptIn(ExperimentalUuidApi::class)
    @Provides
    @SingleIn(AppScope::class)
    fun provideCaptureStore(picturesRoot: Path, clock: Clock): CaptureStore =
        FileCaptureStore(Path(picturesRoot, "captures"), ioDispatcher, clock) { Uuid.random().toString() }

    @OptIn(ExperimentalUuidApi::class)
    @Provides
    @SingleIn(AppScope::class)
    fun provideVehicleRepository(
        database: DrivingLogDatabase,
        clock: Clock,
        deviceTimeZone: DeviceTimeZone,
        pictures: PictureStore,
        @Named("event") eventPictures: PictureStore,
        captures: CaptureStore,
    ): VehicleRepository =
        SqlDelightVehicleRepository(
            database = database,
            clock = clock,
            newId = { Uuid.random().toString() },
            dispatcher = ioDispatcher,
            deviceTimeZone = deviceTimeZone,
            pictures = pictures,
            eventPictures = eventPictures,
            captures = captures,
        )
}

// Metro only rewrites createGraphFactory() in modules with its plugin applied, so the platform shells call this.
fun createAppGraph(
    driver: SqlDriver,
    deviceLocale: DeviceLocale,
    picturesRoot: Path,
    imageCodec: ImageCodec,
    textRecognizer: TextRecognizer,
    authRepository: AuthRepository,
): AppGraph = createGraphFactory<AppGraph.Factory>().create(driver, deviceLocale, picturesRoot, imageCodec, textRecognizer, authRepository)
