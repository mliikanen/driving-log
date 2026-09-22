package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.Vehicle
import com.mikonoma.drivinglog.vehicle.domain.VehicleDetails
import com.mikonoma.drivinglog.vehicle.domain.VehicleEvent
import com.mikonoma.drivinglog.vehicle.domain.VehicleNameOrder
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import com.mikonoma.drivinglog.vehicle.picture.PictureSize
import com.mikonoma.drivinglog.vehicle.picture.VehiclePictureStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce

class LogEventProcessor @AssistedInject constructor(
    @Assisted private val vehicleId: String,
    private val repository: VehicleRepository,
    private val pictures: VehiclePictureStore,
    private val clock: Clock,
    deviceTimeZone: DeviceTimeZone,
) : PresentationProcessor<LogEventIntent, LogEventState, LogEventEffect>(openedState(clock, deviceTimeZone, vehicleId)) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String): LogEventProcessor
    }

    /** True when the form was opened from the Home screen (no vehicle given): it offers the selector and picks a vehicle to start on. False from a vehicle's details screen, which fixes it and never shows the selector. */
    private val chooseVehicle = vehicleId.isEmpty()

    init {
        if (chooseVehicle) {
            // Every vehicle, for the selector, and the one last logged for, to preselect it. The pick below only replaces the chosen id while
            // nothing has been chosen yet, or the chosen vehicle is gone, so it never overrides a restored choice or one the user just made.
            val vehiclesAndRemembered = combine(repository.observeVehicles(), repository.observeLastLoggedVehicleId()) { vehicles, remembered ->
                vehicles.sortedWith(VehicleNameOrder) to remembered
            }
            observe("vehicles", vehiclesAndRemembered) { (ordered, remembered) ->
                val choices = ordered.map { it.toChoice() }
                reduce {
                    val stillThere = ordered.any { it.id == selectedVehicleId }
                    val next = if (stillThere) {
                        selectedVehicleId
                    } else {
                        (remembered?.let { id -> ordered.firstOrNull { it.id == id } } ?: ordered.firstOrNull())?.id.orEmpty()
                    }
                    copy(vehicles = choices, selectedVehicleId = next)
                }
            }
        }

        // The vehicle and its log follow whichever id is chosen now (the constructor's, when fixed; the selector's choice otherwise), so a
        // change of vehicle needs no second code path. Before a choice has been made (chooseVehicle, still loading), this observes nothing.
        val selected: Flow<Pair<VehicleDetails?, List<VehicleEvent>>> = states.map { it.selectedVehicleId }.distinctUntilChanged()
            .flatMapLatest { id ->
                if (id.isEmpty()) flowOf(null to emptyList())
                else combine(repository.observeVehicle(id), repository.observeLog(id)) { details, log -> details to log }
            }
        observe("vehicle", selected) { (details, log) ->
            if (details == null) {
                if (!chooseVehicle) reduce { copy(isLoading = false, notFound = true) }
                // Choosing, with no id yet (or a vehicle that vanished mid-choice): stay loading until the vehicles observer picks one.
            } else {
                val vehicleUnit = details.vehicle.odometerUnit
                // The family starts as the vehicle's; the tenths choice as the one remembered for it, else the vehicle's own.
                val startUnit = unitOf(vehicleUnit.isMiles, details.vehicle.logDistanceTenths ?: vehicleUnit.hasTenths)
                reduce {
                    if (unitFor == details.vehicle.id) {
                        copy(isLoading = false, notFound = false, vehicleUnit = vehicleUnit, log = log)
                    } else {
                        // The unit is (re)initialised from the vehicle now shown; the digits already typed are kept, converted to it,
                        // as when the unit is changed by hand.
                        copy(
                            isLoading = false,
                            notFound = false,
                            vehicleUnit = vehicleUnit,
                            log = log,
                            tripDistance = tripDistance.withUnit(startUnit),
                            newOdometer = newOdometer.withUnit(startUnit),
                            unitFor = details.vehicle.id,
                        )
                    }
                }
            }
        }
    }

    private suspend fun Vehicle.toChoice() = VehicleChoice(id, name, licensePlate, type, color, pictureId?.let { pictures.uri(it, PictureSize.SMALL) })

    override suspend fun map(intent: LogEventIntent): Action<LogEventState, LogEventEffect>? = when (intent) {
        is LogEventIntent.WayChanged -> reduce { copy(way = intent.way, error = null) }
        is LogEventIntent.UnitFamilySelected -> reduce { withUnit(unitOf(intent.miles, unit.hasTenths)) }
        is LogEventIntent.TenthsChanged -> reduce { withUnit(unitOf(unit.isMiles, intent.included)) }
        is LogEventIntent.OdometerEdited -> reduce { withActiveEntry(activeEntry.applyEdit(intent.text)) }
        LogEventIntent.OdometerCleared -> reduce { withActiveEntry(activeEntry.clear(), keepError = true) }
        is LogEventIntent.DateChanged -> reduce { copy(localDateTime = withDate(localDateTime, intent.date), error = null) }
        is LogEventIntent.TimeChanged -> reduce { copy(localDateTime = withTime(localDateTime, intent.hour, intent.minute), error = null) }
        is LogEventIntent.ZoneChanged -> reduce { copy(zoneId = intent.zoneId, error = null) }
        is LogEventIntent.VehicleSelected -> reduce {
            if (chooseVehicle && vehicles.any { it.id == intent.vehicleId }) copy(selectedVehicleId = intent.vehicleId, error = null) else this
        }
        LogEventIntent.Save -> save()
    }

    private fun save(): Action<LogEventState, LogEventEffect>? {
        val form = state
        val vehicleId = form.selectedVehicleId
        if (form.isSaving || form.isLoading || form.notFound || vehicleId.isEmpty()) return null
        val moment = form.moment
        return when (val result = validateLogDistance(form.way, form.activeEntry, moment, clock.now(), form.knownOdometer)) {
            is LogDistanceResult.Invalid -> reduce { copy(error = result.error) }
            is LogDistanceResult.Valid -> saving {
                repository.addDistanceEntry(vehicleId, moment, result.distance, result.loggedOdometer, tenthsIncluded = form.unit.hasTenths)
            }
            is LogDistanceResult.Anchor -> saving {
                repository.addOdometerAnchor(vehicleId, moment, result.reading, tenthsIncluded = form.unit.hasTenths)
            }
        }
    }

    private fun saving(write: suspend () -> Unit): Action<LogEventState, LogEventEffect> = async("save") {
        reduce { copy(isSaving = true, error = null) }
        try {
            write()
        } catch (throwable: Throwable) {
            // Let the user try again; Kide logs the rethrown error.
            reduce { copy(isSaving = false) }
            throw throwable
        }
        emit(LogEventEffect.Saved)
    }

    private companion object {
        /** The form as it is when opened: the time is now, in the device's zone, the unit a placeholder until the vehicle loads, and the
         * vehicle [vehicleId] when the details screen opened it, or none yet (the selector picks one) when the Home screen did. */
        fun openedState(clock: Clock, deviceTimeZone: DeviceTimeZone, vehicleId: String): LogEventState {
            val zone = deviceTimeZone.current()
            return LogEventState(
                tripDistance = OdometerEntry(OdometerUnit.KILOMETERS),
                newOdometer = OdometerEntry(OdometerUnit.KILOMETERS),
                localDateTime = openedAt(clock.now(), zone),
                zoneId = zone.id,
                selectedVehicleId = vehicleId,
            )
        }

        /** The four units are the combinations of kilometers or miles, with or without tenths. */
        fun unitOf(miles: Boolean, tenths: Boolean): OdometerUnit = when {
            !miles && !tenths -> OdometerUnit.KILOMETERS
            !miles -> OdometerUnit.KILOMETERS_TENTHS
            !tenths -> OdometerUnit.MILES
            else -> OdometerUnit.MILES_TENTHS
        }

        /** Both fields change unit together, keeping their digits as when adding a vehicle. */
        fun LogEventState.withUnit(newUnit: OdometerUnit): LogEventState =
            copy(tripDistance = tripDistance.withUnit(newUnit), newOdometer = newOdometer.withUnit(newUnit), error = null)

        /** Replaces the active way's entry; a typed digit clears the error, unless [keepError]. */
        fun LogEventState.withActiveEntry(entry: OdometerEntry, keepError: Boolean = false): LogEventState {
            val error = if (keepError || entry.isEmpty) error else null
            return if (way == LogWay.TRIP_DISTANCE) copy(tripDistance = entry, error = error) else copy(newOdometer = entry, error = error)
        }
    }
}
