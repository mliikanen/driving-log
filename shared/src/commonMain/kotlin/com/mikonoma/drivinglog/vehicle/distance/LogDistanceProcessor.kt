package com.mikonoma.drivinglog.vehicle.distance

import com.mikonoma.drivinglog.vehicle.domain.DeviceTimeZone
import com.mikonoma.drivinglog.vehicle.domain.OdometerUnit
import com.mikonoma.drivinglog.vehicle.domain.VehicleRepository
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.time.Clock
import kotlinx.coroutines.flow.combine
import org.fuusio.kide.presentation.Action
import org.fuusio.kide.presentation.PresentationProcessor
import org.fuusio.kide.presentation.async
import org.fuusio.kide.presentation.reduce

class LogDistanceProcessor @AssistedInject constructor(
    @Assisted private val vehicleId: String,
    private val repository: VehicleRepository,
    private val clock: Clock,
    deviceTimeZone: DeviceTimeZone,
) : PresentationProcessor<LogDistanceIntent, LogDistanceState, LogDistanceEffect>(openedState(clock, deviceTimeZone)) {

    @AssistedFactory
    fun interface Factory {
        fun create(vehicleId: String): LogDistanceProcessor
    }

    init {
        val vehicleAndLog = combine(repository.observeVehicle(vehicleId), repository.observeLog(vehicleId)) { details, log ->
            details to log
        }
        observe("vehicle", vehicleAndLog) { (details, log) ->
            if (details == null) {
                reduce { copy(isLoading = false, notFound = true) }
            } else {
                val vehicleUnit = details.vehicle.odometerUnit
                // The family starts as the vehicle's; the tenths choice as the one remembered for it, else the vehicle's own.
                val startUnit = unitOf(vehicleUnit.isMiles, details.vehicle.logDistanceTenths ?: vehicleUnit.hasTenths)
                reduce {
                    if (unitInitialized) {
                        copy(isLoading = false, notFound = false, vehicleUnit = vehicleUnit, log = log)
                    } else {
                        // The unit starts as the vehicle's; a restored state keeps what the user chose.
                        copy(
                            isLoading = false,
                            notFound = false,
                            vehicleUnit = vehicleUnit,
                            log = log,
                            tripDistance = OdometerEntry(startUnit),
                            newOdometer = OdometerEntry(startUnit),
                            unitInitialized = true,
                        )
                    }
                }
            }
        }
    }

    override suspend fun map(intent: LogDistanceIntent): Action<LogDistanceState, LogDistanceEffect>? = when (intent) {
        is LogDistanceIntent.WayChanged -> reduce { copy(way = intent.way, error = null) }
        is LogDistanceIntent.UnitFamilySelected -> reduce { withUnit(unitOf(intent.miles, unit.hasTenths)) }
        is LogDistanceIntent.TenthsChanged -> reduce { withUnit(unitOf(unit.isMiles, intent.included)) }
        is LogDistanceIntent.OdometerEdited -> reduce { withActiveEntry(activeEntry.applyEdit(intent.text)) }
        LogDistanceIntent.OdometerCleared -> reduce { withActiveEntry(activeEntry.clear(), keepError = true) }
        is LogDistanceIntent.DateChanged -> reduce { copy(localDateTime = withDate(localDateTime, intent.date), error = null) }
        is LogDistanceIntent.TimeChanged -> reduce { copy(localDateTime = withTime(localDateTime, intent.hour, intent.minute), error = null) }
        is LogDistanceIntent.ZoneChanged -> reduce { copy(zoneId = intent.zoneId, error = null) }
        LogDistanceIntent.Save -> save()
    }

    private fun save(): Action<LogDistanceState, LogDistanceEffect>? {
        val form = state
        if (form.isSaving || form.isLoading || form.notFound) return null
        val moment = form.moment
        return when (val result = validateLogDistance(form.way, form.activeEntry, moment, clock.now(), form.knownOdometer)) {
            is LogDistanceResult.Invalid -> reduce { copy(error = result.error) }
            is LogDistanceResult.Valid -> async("save") {
                reduce { copy(isSaving = true, error = null) }
                try {
                    repository.addDistanceEntry(vehicleId, moment, result.distance, result.loggedOdometer, tenthsIncluded = form.unit.hasTenths)
                } catch (throwable: Throwable) {
                    // Let the user try again; Kide logs the rethrown error.
                    reduce { copy(isSaving = false) }
                    throw throwable
                }
                emit(LogDistanceEffect.Saved)
            }
        }
    }

    private companion object {
        /** The form as it is when opened: the time is now, in the device's zone, and the unit is a placeholder until the vehicle loads. */
        fun openedState(clock: Clock, deviceTimeZone: DeviceTimeZone): LogDistanceState {
            val zone = deviceTimeZone.current()
            return LogDistanceState(
                tripDistance = OdometerEntry(OdometerUnit.KILOMETERS),
                newOdometer = OdometerEntry(OdometerUnit.KILOMETERS),
                localDateTime = openedAt(clock.now(), zone),
                zoneId = zone.id,
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
        fun LogDistanceState.withUnit(newUnit: OdometerUnit): LogDistanceState =
            copy(tripDistance = tripDistance.withUnit(newUnit), newOdometer = newOdometer.withUnit(newUnit), error = null)

        /** Replaces the active way's entry; a typed digit clears the error, unless [keepError]. */
        fun LogDistanceState.withActiveEntry(entry: OdometerEntry, keepError: Boolean = false): LogDistanceState {
            val error = if (keepError || entry.isEmpty) error else null
            return if (way == LogWay.TRIP_DISTANCE) copy(tripDistance = entry, error = error) else copy(newOdometer = entry, error = error)
        }
    }
}
