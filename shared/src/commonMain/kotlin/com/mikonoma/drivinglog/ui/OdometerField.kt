package com.mikonoma.drivinglog.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.mikonoma.drivinglog.locale.NumberSymbols
import com.mikonoma.drivinglog.vehicle.format.formatSteps
import com.mikonoma.drivinglog.vehicle.input.OdometerEntry

/**
 * A microwave-style odometer field. It uses the system's number keyboard: the text underneath is just the
 * digits of the entry, and each digit typed enters at the right-hand end. The decimal (for the tenths units)
 * and thousands separators are drawn by [symbols], so the user never types one. The number is right-aligned
 * and the unit abbreviation stays fixed at the end of the field.
 */
@Composable
fun OdometerField(
    entry: OdometerEntry,
    symbols: NumberSymbols,
    onEdit: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Odometer",
) {
    val digits = entry.digits
    OutlinedTextField(
        // The cursor always sits at the end: digits can only be added or removed there.
        value = TextFieldValue(text = digits, selection = TextRange(digits.length)),
        onValueChange = { onEdit(it.text) },
        modifier = modifier.fillMaxWidth().testTag("odo_field"),
        label = { Text(label) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = OdometerTransformation(entry.unit.hasTenths, symbols),
        suffix = { Text(entry.unit.abbreviation) },
        trailingIcon = {
            IconButton(onClick = onClear, modifier = Modifier.testTag("odo_clear")) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear odometer")
            }
        },
    )
}

/** Draws the entry's digits as a locale-formatted number. Offsets all map to the end, where the cursor is. */
private class OdometerTransformation(
    private val hasTenths: Boolean,
    private val symbols: NumberSymbols,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = formatSteps(text.text.toLongOrNull() ?: 0L, hasTenths, symbols)
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
