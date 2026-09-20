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
//
// Every key must also have value-based equals/hashCode/toString (serialKey plus its arguments, never the graph):
// Kide keeps a screen's processor in a store keyed by the key, and after a rotation the back stack is restored into
// NEW key instances. With identity equality they would not match the old ones, every rotation would build a fresh
// processor, and whatever the user had typed would be lost.

class VehicleListNavKey(private val graph: AppGraph) : ScreenNavKey<VehicleListProcessor> {
    override val serialKey: String = "vehicle-list"

    override fun equals(other: Any?): Boolean = other is VehicleListNavKey

    override fun hashCode(): Int = serialKey.hashCode()

    override fun toString(): String = "VehicleListNavKey"

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

    override fun equals(other: Any?): Boolean = other is AddVehicleNavKey

    override fun hashCode(): Int = serialKey.hashCode()

    override fun toString(): String = "AddVehicleNavKey"

    override fun createProcessor(): AddVehicleProcessor = graph.addVehicleProcessor

    override val stateSerializer: KSerializer<out ViewState> get() = AddVehicleState.serializer()

    override val screen: @Composable (ScreenContext<AddVehicleProcessor>) -> Unit = { ctx ->
        AddVehicleScreen(ctx.processor, graph.deviceLocale, onBack = ctx.onBack)
    }
}

class VehicleDetailsNavKey(private val graph: AppGraph, val vehicleId: String = "") : ScreenNavKey<VehicleDetailsProcessor> {
    override val serialKey: String = "vehicle-details"

    override fun equals(other: Any?): Boolean = other is VehicleDetailsNavKey && other.vehicleId == vehicleId

    override fun hashCode(): Int = 31 * serialKey.hashCode() + vehicleId.hashCode()

    override fun toString(): String = "VehicleDetailsNavKey(" + vehicleId + ")"

    override fun createProcessor(): VehicleDetailsProcessor = graph.vehicleDetailsProcessorFactory.create(vehicleId)

    override fun saveArgs(): String = vehicleId

    override fun restoreArgs(args: String): ScreenNavKey<VehicleDetailsProcessor> = VehicleDetailsNavKey(graph, args)

    override val screen: @Composable (ScreenContext<VehicleDetailsProcessor>) -> Unit = { ctx ->
        VehicleDetailsScreen(
            processor = ctx.processor,
            deviceLocale = graph.deviceLocale,
            deviceTimeZone = graph.deviceTimeZone,
            onShowEdit = { id -> ctx.navigateTo(EditVehicleNavKey(graph, id)) },
            onShowLog = { id -> ctx.navigateTo(VehicleLogNavKey(graph, id)) },
            onBack = ctx.onBack,
        )
    }
}

class EditVehicleNavKey(private val graph: AppGraph, val vehicleId: String = "") : ScreenNavKey<EditVehicleProcessor> {
    override val serialKey: String = "vehicle-edit"

    override fun equals(other: Any?): Boolean = other is EditVehicleNavKey && other.vehicleId == vehicleId

    override fun hashCode(): Int = 31 * serialKey.hashCode() + vehicleId.hashCode()

    override fun toString(): String = "EditVehicleNavKey(" + vehicleId + ")"

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

    override fun equals(other: Any?): Boolean = other is VehicleLogNavKey && other.vehicleId == vehicleId

    override fun hashCode(): Int = 31 * serialKey.hashCode() + vehicleId.hashCode()

    override fun toString(): String = "VehicleLogNavKey(" + vehicleId + ")"

    override fun createProcessor(): VehicleLogProcessor = graph.vehicleLogProcessorFactory.create(vehicleId)

    override fun saveArgs(): String = vehicleId

    override fun restoreArgs(args: String): ScreenNavKey<VehicleLogProcessor> = VehicleLogNavKey(graph, args)

    override val screen: @Composable (ScreenContext<VehicleLogProcessor>) -> Unit = { ctx ->
        VehicleLogScreen(ctx.processor, graph.deviceLocale, graph.deviceTimeZone, onBack = ctx.onBack)
    }
}

private var registeredFor: AppGraph? = null

/**
 * Registers a prototype of every vehicle screen so Kide can restore the back stack after process death.
 *
 * The registry is process-wide and rejects a serialKey it already has, while this is called from composition, which
 * runs again whenever the activity is recreated (a rotation, say). So it registers once per graph and is a no-op after.
 */
fun registerVehicleNavKeys(graph: AppGraph) {
    if (registeredFor === graph) return
    ScreenNavKeyRegistry.clear()
    ScreenNavKeyRegistry.register(VehicleListNavKey(graph))
    ScreenNavKeyRegistry.register(AddVehicleNavKey(graph))
    ScreenNavKeyRegistry.register(VehicleDetailsNavKey(graph))
    ScreenNavKeyRegistry.register(EditVehicleNavKey(graph))
    ScreenNavKeyRegistry.register(VehicleLogNavKey(graph))
    registeredFor = graph
}
