package com.mikonoma.drivinglog.vehicle.ocr

internal val ODOMETER_LABELS = setOf("ODO", "ODOMETER", "TOTAL DISTANCE")
internal val TRIP_LABELS = setOf("TRIP", "TRIP A", "TRIP B", "T")
internal val DISTANCE_LABEL_KINDS: Map<String, ReadingKind> =
    ODOMETER_LABELS.associateWith { ReadingKind.ODOMETER } + TRIP_LABELS.associateWith { ReadingKind.TRIP }

/**
 * Recognized volume words next to a number mean a fuel amount (`add-fuel-amount-ocr`, design.md's evaluation):
 * `LITRAA`/`LITARA`/`LITROV` (Finnish, Croatian, Slovenian), `dm` and `dm^3` (Polish; 1 dm³ is exactly 1 liter,
 * not deciliters), and `L`/`LITERS`/`LITRES`/`GAL`/`GALLON`/`GALLONS` as an untested English/US hypothesis — no
 * real English-labeled photo was available to check those against. There is no separate list of price labels to
 * exclude by: a price or a price-per-unit number is simply never next to one of these words either (the
 * evaluation found no photo where it was), so absence of a match already excludes it.
 */
internal val VOLUME_LABELS = setOf("LITRAA", "LITARA", "LITROV", "DM^3", "DM", "L", "LITERS", "LITRES", "GAL", "GALLON", "GALLONS")
internal val VOLUME_LABEL_KINDS: Map<String, ReadingKind> = VOLUME_LABELS.associateWith { ReadingKind.FUEL_AMOUNT }

/** The kind [labels] names for a label, comparing case-insensitively, zero read as O and spaces collapsed; null when [text] names none of them. */
internal fun labelKind(text: String, labels: Map<String, ReadingKind>): ReadingKind? {
    val normalized = text.uppercase().replace('0', 'O').split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    return labels[normalized]
}

/**
 * The label of the number at [index] of [line], and the kind it names from [labels]: the one or two words directly before it on its
 * line, or a whole line directly above or below it (design.md). The nearest wins when several qualify. A lone "T" counts only on the
 * number's own line: above or below it, it is as likely a mode icon beside another figure (on `odo/20220911_162029` it sits under the
 * clock) — harmless to exclude when [labels] has no entry shaped like a lone letter anyway (`add-fuel-amount-ocr`'s [VOLUME_LABEL_KINDS]).
 */
internal fun labelOf(photo: RecognizedPhoto, line: RecognizedLine, index: Int, box: TextBox, labels: Map<String, ReadingKind>): Pair<String, ReadingKind>? {
    for (count in 2 downTo 1) {
        if (index - count < 0) continue
        val words = line.elements.subList(index - count, index).joinToString(" ") { it.text }
        labelKind(words, labels)?.let { return words to it }
    }
    val maxGap = box.height * ReadingThresholds.LABEL_GAP_HEIGHTS
    return photo.lines.asSequence()
        .filter { it !== line }
        .filter { !isLoneT(it.text) }
        .mapNotNull { other -> labelKind(other.text, labels)?.let { Triple(other, it, verticalGap(other.box, box)) } }
        .filter { (other, _, gap) -> gap <= maxGap && horizontallyNear(other.box, box) }
        .minByOrNull { (_, _, gap) -> gap }
        ?.let { (other, kind, _) -> other.text to kind }
}

internal fun isLoneT(text: String): Boolean = text.trim().equals("T", ignoreCase = true)
