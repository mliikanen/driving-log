package com.mikonoma.drivinglog.vehicle.distance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikonoma.drivinglog.locale.DeviceLocale
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.ui.OdometerField
import com.mikonoma.drivinglog.vehicle.domain.FuelUnit
import com.mikonoma.drivinglog.vehicle.format.formatFuelSteps
import com.mikonoma.drivinglog.vehicle.format.formatOdometer
import com.mikonoma.drivinglog.vehicle.format.formatTimeOfDay

@Composable
internal fun MomentRow(state: LogEventState, deviceLocale: DeviceLocale, onDate: () -> Unit, onTime: () -> Unit, onZone: () -> Unit) {
    val local = state.localDateTime
    val offset = state.moment.zone?.offsetSeconds ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Date, time and time zone", style = MaterialTheme.typography.titleSmall)
        // Wraps only when the three buttons do not fit on one row; all share one height either way.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MomentButton(onDate, "log_date") {
                Text(local.date.toString())
                SubtleText(deviceLocale.weekdayName(local.date.dayOfWeek))
            }
            MomentButton(onTime, "log_time") {
                Text(formatTimeOfDay(local.hour, local.minute, deviceLocale.timeFormat()))
            }
            MomentButton(onZone, "log_zone") {
                Text(state.zoneId)
                SubtleText(formatUtcOffset(offset))
            }
        }
    }
}

internal val MomentButtonHeight = 64.dp

@Composable
internal fun MomentButton(onClick: () -> Unit, testTag: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.height(MomentButtonHeight).testTag(testTag)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)
    }
}

/**
 * A refueling's fuel amount field (`add-refueling-logging`): the same microwave-style digit entry
 * [com.mikonoma.drivinglog.ui.OdometerField] uses, but always two decimal places, whatever [unit] is (its own entry
 * widget, per design.md's decision not to reuse [com.mikonoma.drivinglog.vehicle.input.OdometerEntry]'s
 * unit-driven precision).
 */
@Composable
internal fun FuelAmountField(
    entry: com.mikonoma.drivinglog.vehicle.input.FuelAmountEntry,
    unit: FuelUnit,
    symbols: NumberSymbols,
    onEdit: (String) -> Unit,
    onClear: () -> Unit,
    isError: Boolean,
    errorText: String?,
) {
    val digits = entry.digits
    OutlinedTextField(
        value = TextFieldValue(text = digits, selection = TextRange(digits.length)),
        onValueChange = { onEdit(it.text) },
        modifier = Modifier.fillMaxWidth().testTag("fuel_amount_field"),
        label = { Text("Fuel amount *") },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) ({ Text(errorText) }) else null,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = FuelAmountTransformation(symbols),
        suffix = { Text(unit.abbreviation) },
        trailingIcon = {
            IconButton(onClick = onClear, modifier = Modifier.testTag("fuel_amount_clear")) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear fuel amount")
            }
        },
    )
}

/** Draws the entry's digits as a locale-formatted, always-two-decimal number. Offsets all map to the end, where the cursor is. */
internal class FuelAmountTransformation(private val symbols: NumberSymbols) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = text.text.toLongOrNull()?.let { formatFuelSteps(it, symbols) } ?: ""
        val original = text.text.length
        return TransformedText(
            AnnotatedString(shown),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = shown.length
                override fun transformedToOriginal(offset: Int): Int = original
            },
        )
    }
}

@Composable
internal fun KnownOdometerInfo(state: LogEventState, symbols: com.mikonoma.drivinglog.locale.NumberSymbols) {
    val known = state.knownOdometer
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (known == null) {
            // Not an error: the count is saved as an odometer anchor, a new starting point for the odometer.
            Text(
                "No odometer is known at this time. The count will be saved as a new odometer starting point.",
                modifier = Modifier.testTag("log_no_known_odometer"),
            )
        } else {
            Text(
                "Previous known odometer: " + formatOdometer(known, state.vehicleUnit, symbols),
                modifier = Modifier.testTag("log_known_odometer"),
            )
            state.previewDistance?.let { distance ->
                Text("Distance: " + formatOdometer(distance, state.vehicleUnit, symbols), modifier = Modifier.testTag("log_live_distance"))
            }
        }
    }
}

/** A secondary line under a button's main text: smaller and lighter, so the main text stays the first thing read. */
@Composable
internal fun SubtleText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

@Composable
internal fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("log_error"))
}

internal fun errorMessage(state: LogEventState, symbols: com.mikonoma.drivinglog.locale.NumberSymbols): String = when (val error = state.error) {
    null -> ""

    LogDistanceError.FieldEmpty ->
        if (state.way == LogWay.TRIP_DISTANCE) "Enter the trip distance" else "Enter the odometer reading"

    LogDistanceError.TimeInFuture -> "The time cannot be in the future"

    LogDistanceError.DistanceNotPositive -> "The distance must be more than zero"

    is LogDistanceError.OdometerNotHigher -> "Enter a reading higher than " + formatOdometer(error.known, state.vehicleUnit, symbols)

    LogDistanceError.FuelAmountEmpty -> "Enter the fuel amount"

    LogDistanceError.FuelAmountNotPositive -> "The fuel amount must be more than zero"
}
