package com.mikonoma.drivinglog.vehicle

import androidx.compose.runtime.Composable
import com.mikonoma.drivinglog.di.AppGraph
import com.mikonoma.drivinglog.landing.LandingNavKey
import com.mikonoma.drivinglog.vehicle.add.AddVehicleProcessor
import com.mikonoma.drivinglog.vehicle.add.AddVehicleScreen
import com.mikonoma.drivinglog.vehicle.add.AddVehicleState
import com.mikonoma.drivinglog.vehicle.details.VehicleDetailsProcessor
import com.mikonoma.drivinglog.vehicle.details.VehicleDetailsScreen
import com.mikonoma.drivinglog.vehicle.distance.LogEventProcessor
import com.mikonoma.drivinglog.vehicle.distance.LogEventScreen
import com.mikonoma.drivinglog.vehicle.distance.LogEventState
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleProcessor
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleScreen
import com.mikonoma.drivinglog.vehicle.edit.EditVehicleState
import com.mikonoma.drivinglog.vehicle.eventdetails.EventDetailsProcessor
import com.mikonoma.drivinglog.vehicle.eventdetails.EventDetailsScreen
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
            onBack = ctx.onBack,
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
            onShowLogEvent = { id -> ctx.navigateTo(LogEventNavKey(graph, id)) },
            onShowEventDetails = { vId, eId -> ctx.navigateTo(EventDetailsNavKey(graph, vId, eId)) },
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
        VehicleLogScreen(
            processor = ctx.processor,
            deviceLocale = graph.deviceLocale,
            deviceTimeZone = graph.deviceTimeZone,
            onShowEventDetails = { eventId -> ctx.navigateTo(EventDetailsNavKey(graph, vehicleId, eventId)) },
            onBack = ctx.onBack,
        )
    }
}

/** Its [serialKey] keeps the old word ("vehicle-log-distance", from when the form only logged distances) on purpose: it is what a back stack saved by an earlier
 * build has stored, and changing it would strand that saved key. Everything else here follows the form's current name, "Log event". */
class LogEventNavKey(private val graph: AppGraph, val vehicleId: String = "") : ScreenNavKey<LogEventProcessor> {
    override val serialKey: String = "vehicle-log-distance"

    override fun equals(other: Any?): Boolean = other is LogEventNavKey && other.vehicleId == vehicleId

    override fun hashCode(): Int = 31 * serialKey.hashCode() + vehicleId.hashCode()

    override fun toString(): String = "LogEventNavKey(" + vehicleId + ")"

    override fun createProcessor(): LogEventProcessor = graph.logEventProcessorFactory.create(vehicleId)

    override val stateSerializer: KSerializer<out ViewState> get() = LogEventState.serializer()

    override fun saveArgs(): String = vehicleId

    override fun restoreArgs(args: String): ScreenNavKey<LogEventProcessor> = LogEventNavKey(graph, args)

    override val screen: @Composable (ScreenContext<LogEventProcessor>) -> Unit = { ctx ->
        LogEventScreen(ctx.processor, graph.deviceLocale, graph.deviceTimeZone, onBack = ctx.onBack)
    }
}

/**
 * Opens a logged event's read-only details (add-event-details-view). [vehicleId] and [eventId] are packed into one
 * [saveArgs] string (`"$vehicleId|$eventId"`) since [ScreenNavKey.saveArgs] carries only one string; both ids are
 * plain UUIDs (`Uuid.random().toString()`), which never contain `|`.
 */
class EventDetailsNavKey(private val graph: AppGraph, val vehicleId: String = "", val eventId: String = "") : ScreenNavKey<EventDetailsProcessor> {
    override val serialKey: String = "event-details"

    override fun equals(other: Any?): Boolean = other is EventDetailsNavKey && other.vehicleId == vehicleId && other.eventId == eventId

    override fun hashCode(): Int = 31 * (31 * serialKey.hashCode() + vehicleId.hashCode()) + eventId.hashCode()

    override fun toString(): String = "EventDetailsNavKey($vehicleId, $eventId)"

    override fun createProcessor(): EventDetailsProcessor = graph.eventDetailsProcessorFactory.create(vehicleId, eventId)

    override fun saveArgs(): String = "$vehicleId|$eventId"

    override fun restoreArgs(args: String): ScreenNavKey<EventDetailsProcessor> {
        val (savedVehicleId, savedEventId) = args.split("|", limit = 2)
        return EventDetailsNavKey(graph, savedVehicleId, savedEventId)
    }

    override val screen: @Composable (ScreenContext<EventDetailsProcessor>) -> Unit = { ctx ->
        EventDetailsScreen(ctx.processor, graph.deviceLocale, graph.deviceTimeZone, onBack = ctx.onBack)
    }
}

private var registeredFor: AppGraph? = null

/**
 * Registers a prototype of every screen (the Home screen and the vehicle screens) so Kide can restore the back stack after process death.
 *
 * The registry is process-wide and rejects a serialKey it already has, while this is called from composition, which
 * runs again whenever the activity is recreated (a rotation, say). So it registers once per graph and is a no-op after.
 */
fun registerVehicleNavKeys(graph: AppGraph) {
    if (registeredFor === graph) return
    ScreenNavKeyRegistry.clear()
    ScreenNavKeyRegistry.register(LandingNavKey(graph))
    ScreenNavKeyRegistry.register(VehicleListNavKey(graph))
    ScreenNavKeyRegistry.register(AddVehicleNavKey(graph))
    ScreenNavKeyRegistry.register(VehicleDetailsNavKey(graph))
    ScreenNavKeyRegistry.register(EditVehicleNavKey(graph))
    ScreenNavKeyRegistry.register(VehicleLogNavKey(graph))
    ScreenNavKeyRegistry.register(LogEventNavKey(graph))
    ScreenNavKeyRegistry.register(EventDetailsNavKey(graph))
    registeredFor = graph
}
