package com.mikonoma.drivinglog.vehicle

import androidx.compose.runtime.Composable
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.vehicle.add.AddVehicleProcessor
import com.mikonoma.drivinglog.vehicle.add.AddVehicleScreen
import com.mikonoma.drivinglog.vehicle.add.AddVehicleState
import com.mikonoma.drivinglog.vehicle.details.VehicleDetailsProcessor
import com.mikonoma.drivinglog.vehicle.details.VehicleDetailsScreen
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleProcessor
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleScreen
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleState
import com.mikonoma.drivinglog.vehicle.list.VehicleListProcessor
import com.mikonoma.drivinglog.vehicle.list.VehicleListScreen
import com.mikonoma.drivinglog.vehicle.log.VehicleLogProcessor
import com.mikonoma.drivinglog.vehicle.log.VehicleLogScreen
import kotlinx.serialization.KSerializer
import org.fuusio.kide.navigation.ScreenContext
import org.fuusio.kide.navigation.ScreenNavKey
import org.fuusio.kide.navigation.ScreenNavKeyRegistry
import org.fuusio.kide.presentation.ViewState

// Kide restores a screen after process death by looking its key up in ScreenNavKeyRegistry by serialKey and
// calling restoreArgs, so every key carries the graph it builds its processor from.

class VehicleListNavKey(private val graph: AppGraph) : ScreenNavKey<VehicleListProcessor> {
    override val serialKey: String = "vehicle-list"

    override fun createProcessor(): VehicleListProcessor = graph.vehicleListProcessor

    override val screen: @Composable (ScreenContext<VehicleListProcessor>) -> Unit = { ctx ->
        VehicleListScreen(
            processor = ctx.processor,
            onShowDetails = { id -> ctx.navigateTo(VehicleDetailsNavKey(graph, id)) },
            onShowAdd = { ctx.navigateTo(AddVehicleNavKey(graph)) },
        )
    }
}

class AddVehicleNavKey(private val graph: AppGraph) : ScreenNavKey<AddVehicleProcessor> {
    override val serialKey: String = "vehicle-add"

    override fun createProcessor(): AddVehicleProcessor = graph.addVehicleProcessor

    override val stateSerializer: KSerializer<out ViewState> get() = AddVehicleState.serializer()

    override val screen: @Composable (ScreenContext<AddVehicleProcessor>) -> Unit = { ctx ->
        AddVehicleScreen(ctx.processor, graph.deviceLocale, onBack = ctx.onBack)
    }
}

class VehicleDetailsNavKey(private val graph: AppGraph, val vehicleId: String = "") : ScreenNavKey<VehicleDetailsProcessor> {
    override val serialKey: String = "vehicle-details"

    override fun createProcessor(): VehicleDetailsProcessor = graph.vehicleDetailsProcessorFactory.create(vehicleId)

    override fun saveArgs(): String = vehicleId

    override fun restoreArgs(args: String): ScreenNavKey<VehicleDetailsProcessor> = VehicleDetailsNavKey(graph, args)

    override val screen: @Composable (ScreenContext<VehicleDetailsProcessor>) -> Unit = { ctx ->
        VehicleDetailsScreen(
            processor = ctx.processor,
            deviceLocale = graph.deviceLocale,
            onShowEdit = { id -> ctx.navigateTo(EditVehicleNavKey(graph, id)) },
            onShowLog = { id -> ctx.navigateTo(VehicleLogNavKey(graph, id)) },
            onBack = ctx.onBack,
        )
    }
}

class EditVehicleNavKey(private val graph: AppGraph, val vehicleId: String = "") : ScreenNavKey<EditVehicleProcessor> {
    override val serialKey: String = "vehicle-edit"

    override fun createProcessor(): EditVehicleProcessor = graph.editVehicleProcessorFactory.create(vehicleId)

    override val stateSerializer: KSerializer<out ViewState> get() = EditVehicleState.serializer()

    override fun saveArgs(): String = vehicleId

    override fun restoreArgs(args: String): ScreenNavKey<EditVehicleProcessor> = EditVehicleNavKey(graph, args)

    override val screen: @Composable (ScreenContext<EditVehicleProcessor>) -> Unit = { ctx ->
        EditVehicleScreen(ctx.processor, onBack = ctx.onBack)
    }
}

class VehicleLogNavKey(private val graph: AppGraph, val vehicleId: String = "") : ScreenNavKey<VehicleLogProcessor> {
    override val serialKey: String = "vehicle-log"

    override fun createProcessor(): VehicleLogProcessor = graph.vehicleLogProcessorFactory.create(vehicleId)

    override fun saveArgs(): String = vehicleId

    override fun restoreArgs(args: String): ScreenNavKey<VehicleLogProcessor> = VehicleLogNavKey(graph, args)

    override val screen: @Composable (ScreenContext<VehicleLogProcessor>) -> Unit = { ctx ->
        VehicleLogScreen(ctx.processor, graph.deviceLocale, onBack = ctx.onBack)
    }
}

/** Registers a prototype of every vehicle screen so Kide can restore the back stack after process death. */
fun registerVehicleNavKeys(graph: AppGraph) {
    ScreenNavKeyRegistry.register(VehicleListNavKey(graph))
    ScreenNavKeyRegistry.register(AddVehicleNavKey(graph))
    ScreenNavKeyRegistry.register(VehicleDetailsNavKey(graph))
    ScreenNavKeyRegistry.register(EditVehicleNavKey(graph))
    ScreenNavKeyRegistry.register(VehicleLogNavKey(graph))
}
