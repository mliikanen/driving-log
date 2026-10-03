package com.mikonoma.drivinglog.vehicle.ocr

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

/** [sameRowNearby]'s sideways reach: one and a half box heights, counted in half heights. */
private const val MAX_GAP_HALF_HEIGHTS = 3

/** What a detected reading is taken to be: the odometer's count, a trip meter's distance, or a refueling's fuel amount. */
@Serializable
enum class ReadingKind { ODOMETER, TRIP, FUEL_AMOUNT }

/** Why a number was classified the way it was, kept with the detections to review a misdetection later. */
@Serializable
enum class DetectionBasis {
    /** A label next to it decided the kind. */
    LABEL,

    /** No label; its magnitude against the known odometer decided the kind. */
    MAGNITUDE,

    /** No label, and no fallback rule classifies it either: for an odometer/trip reading, no distance unit after
     * it and not plausible as the odometer nor a trip with tenths (a dial number, a clock); for a fuel amount
     * (which has no fallback rule at all), simply no recognized volume label next to it. Not presented either way. */
    NO_UNIT,

    /** No label, and neither close to the known odometer nor plausible as a trip: not presented. */
    IMPLAUSIBLE,
}

/**
 * One number found in a scanned photo (`odometer-ocr-capture`). Every number that could be a reading is kept, the ones not
 * presented to the user too ([kind] null), since the detections exist to review a misdetection after the fact.
 */
@Serializable
data class Detection(
    /** The word as it was recognized, unit or noise included (`71140km`). */
    val text: String,
    /** The number it reads as, digits with at most one `.` (`71140`, `168.1`). */
    val value: String,
    val box: TextBox,
    /** Odometer- or trip-like, or null when it is not presented as a candidate. */
    val kind: ReadingKind?,
    val basis: DetectionBasis,
    /** The label that decided [kind], as recognized, when [basis] is [DetectionBasis.LABEL]. */
    val label: String? = null,
    /** A distance unit followed the number (`71140km`, `917 km`). False in a result stored before this was kept (`scan-initial-odometer`). */
    val hasUnit: Boolean = false,
) {
    /** The whole units of [value] (`168` of `168.1`). */
    val whole: Long get() = value.substringBefore('.').toLong()

    /** The first digit after the decimal point, or null when [value] has none. */
    val tenth: Int? get() = value.substringAfter('.', "").firstOrNull()?.digitToInt()
}

/**
 * The thresholds of the classification (design.md, "Classification"), in the vehicle's odometer unit, in one place so that tuning them
 * against real photos is one edit.
 */
object ReadingThresholds {
    /** An unlabeled value at most this is a plausible trip distance. */
    const val MAX_TRIP = 2000.0

    /** An unlabeled value up to this much above the known odometer is a plausible odometer count... */
    const val MIN_ODOMETER_MARGIN = 5000.0

    /** ...or up to this fraction of the known odometer above it, whichever is more. */
    const val ODOMETER_MARGIN_FRACTION = 0.5

    /** A label line counts when its vertical gap to the number is at most this many of the number's heights. */
    const val LABEL_GAP_HEIGHTS = 1.5

    /** A number is a candidate from this many digits before any decimal point, leading zeros not counted... */
    const val MIN_DIGITS = 3

    /** ...up to this many: the most an odometer field holds. */
    const val MAX_DIGITS = 7

    /** Two detections are at the same place when they share at least this much of the smaller one's box. */
    const val SAME_PLACE = 0.3

    /** A fuel-amount candidate's whole-digit count (`add-fuel-amount-ocr`): distinct from the odometer bounds
     * above — a single refueling is realistically a few liters/gallons up to a large tank, nothing like an
     * odometer's multi-year count, so a 5+ digit number (and most 3-digit ones) is never a fuel amount. */
    const val MIN_FUEL_AMOUNT_DIGITS = 1
    const val MAX_FUEL_AMOUNT_DIGITS = 4
}

private val ODOMETER_LABELS = setOf("ODO", "ODOMETER", "TOTAL DISTANCE")
private val TRIP_LABELS = setOf("TRIP", "TRIP A", "TRIP B", "T")
private val DISTANCE_LABEL_KINDS: Map<String, ReadingKind> =
    ODOMETER_LABELS.associateWith { ReadingKind.ODOMETER } + TRIP_LABELS.associateWith { ReadingKind.TRIP }

/**
 * Recognized volume words next to a number mean a fuel amount (`add-fuel-amount-ocr`, design.md's evaluation):
 * `LITRAA`/`LITARA`/`LITROV` (Finnish, Croatian, Slovenian), `dm` and `dm^3` (Polish; 1 dm³ is exactly 1 liter,
 * not deciliters), and `L`/`LITERS`/`LITRES`/`GAL`/`GALLON`/`GALLONS` as an untested English/US hypothesis — no
 * real English-labeled photo was available to check those against. There is no separate list of price labels to
 * exclude by: a price or a price-per-unit number is simply never next to one of these words either (the
 * evaluation found no photo where it was), so absence of a match already excludes it.
 */
private val VOLUME_LABELS = setOf("LITRAA", "LITARA", "LITROV", "DM^3", "DM", "L", "LITERS", "LITRES", "GAL", "GALLON", "GALLONS")
private val VOLUME_LABEL_KINDS: Map<String, ReadingKind> = VOLUME_LABELS.associateWith { ReadingKind.FUEL_AMOUNT }

/** Digits, at most one decimal separator with one or two digits after it, and letters run directly onto it (a unit, or noise). */
private val NUMBER = Regex("""^(\d+)(?:[.,](\d{1,2}))?([A-Za-z]*)$""")

/**
 * Every number in [photo] that could be a reading, each classified (design.md): a label next to it decides first; otherwise its magnitude
 * against [knownOdometer] (the known odometer at the entry's time, in the vehicle's odometer unit, or null when none is known then),
 * fully when a distance unit follows it, and without one only as an odometer reading, or as a trip when it has a decimal
 * (`add-seven-segment-ocr`: LCD readings often come back without unit or label; dial numbers are whole numbers). The rest are kept with a
 * null kind. Where two recognizers read the same place, one detection is kept ([onePerPlace]).
 */
fun detectReadings(photo: RecognizedPhoto, knownOdometer: Double?): List<Detection> = photo.lines.flatMap { line ->
    line.elements.mapIndexedNotNull { index, element ->
        val match = NUMBER.matchEntire(element.text) ?: return@mapIndexedNotNull null
        val (wholeDigits, fraction, attached) = match.destructured
        if (wholeDigits.trimStart('0').length !in ReadingThresholds.MIN_DIGITS..ReadingThresholds.MAX_DIGITS) return@mapIndexedNotNull null
        val value = if (fraction.isEmpty()) wholeDigits.trimStart('0') else "${wholeDigits.trimStart('0')}.$fraction"
        val label = labelOf(photo, line, index, element.box, DISTANCE_LABEL_KINDS)
        val hasUnit = isDistanceUnit(attached) || isDistanceUnit(line.elements.getOrNull(index + 1)?.text.orEmpty())
        val byMagnitude = kindByMagnitude(value.toDouble(), knownOdometer)
        when {
            label != null -> Detection(element.text, value, element.box, label.second, DetectionBasis.LABEL, label.first, hasUnit)

            hasUnit -> Detection(
                element.text,
                value,
                element.box,
                byMagnitude,
                if (byMagnitude ==
                    null
                ) {
                    DetectionBasis.IMPLAUSIBLE
                } else {
                    DetectionBasis.MAGNITUDE
                },
                hasUnit = true,
            )

            byMagnitude == ReadingKind.ODOMETER || (byMagnitude == ReadingKind.TRIP && fraction.isNotEmpty()) ->
                Detection(element.text, value, element.box, byMagnitude, DetectionBasis.MAGNITUDE)

            else -> Detection(element.text, value, element.box, null, DetectionBasis.NO_UNIT)
        }
    }
}.onePerPlace()

/**
 * The readings of [photo] as a new vehicle's odometer (`scan-initial-odometer`): with no known odometer to compare with, a number labeled
 * as the odometer, or unlabeled with a distance unit after it whatever its size, is offered as the odometer; one labeled as a trip is
 * not offered, nor an unlabeled one without a unit of [ReadingThresholds.MAX_TRIP] or less (a dial's scale, a clock). Labels, units and
 * [onePerPlace] work as in [detectReadings]; only the kinds differ.
 */
fun detectInitialOdometer(photo: RecognizedPhoto): List<Detection> = detectReadings(photo, knownOdometer = null).map { d ->
    val kind = when {
        d.basis == DetectionBasis.LABEL -> d.kind.takeIf { it == ReadingKind.ODOMETER }
        d.hasUnit -> ReadingKind.ODOMETER
        d.value.toDouble() > ReadingThresholds.MAX_TRIP -> ReadingKind.ODOMETER
        else -> null
    }
    val basis = when {
        d.basis == DetectionBasis.LABEL -> DetectionBasis.LABEL
        kind != null -> DetectionBasis.MAGNITUDE
        else -> DetectionBasis.NO_UNIT
    }
    d.copy(kind = kind, basis = basis)
}

/**
 * Every number in [photo] that could be a refueling's fuel amount (`add-fuel-amount-ocr`, design.md), classified
 * by a recognized volume label next to it only: unlike [detectReadings], there is no magnitude fallback at all —
 * no known fuel amount exists to compare against the way a known odometer does, so an unlabeled number is never a
 * candidate, however plausible its size. Shares [labelOf]'s label-adjacency rule and [onePerPlace]'s same-place
 * merging with [detectReadings], and uses its own, smaller digit-count bounds
 * ([ReadingThresholds.MIN_FUEL_AMOUNT_DIGITS]/[ReadingThresholds.MAX_FUEL_AMOUNT_DIGITS]): a realistic fuel amount
 * is one or two digits, not the three-to-seven an odometer reading needs to even be considered.
 */
fun detectFuelAmount(photo: RecognizedPhoto): List<Detection> = photo.lines.flatMap { line ->
    line.elements.mapIndexedNotNull { index, element ->
        val match = NUMBER.matchEntire(element.text) ?: return@mapIndexedNotNull null
        val (wholeDigits, fraction, _) = match.destructured
        if (wholeDigits.trimStart('0').length !in
            ReadingThresholds.MIN_FUEL_AMOUNT_DIGITS..ReadingThresholds.MAX_FUEL_AMOUNT_DIGITS
        ) {
            return@mapIndexedNotNull null
        }
        val value = if (fraction.isEmpty()) wholeDigits.trimStart('0') else "${wholeDigits.trimStart('0')}.$fraction"
        val label = labelOf(photo, line, index, element.box, VOLUME_LABEL_KINDS)
        if (label != null) {
            Detection(element.text, value, element.box, label.second, DetectionBasis.LABEL, label.first)
        } else {
            Detection(element.text, value, element.box, null, DetectionBasis.NO_UNIT)
        }
    }
}.onePerPlace()

/**
 * One detection per place in the photo: where two overlap (their intersection is at least [ReadingThresholds.SAME_PLACE] of the smaller
 * box, or they read the same value on the same row close by: PP-OCR's word boxes are estimated from its line's box, so its box for a
 * number can sit beside the other recognizer's rather than on it), the one with more
 * digits is kept (ML Kit's `71140` over PP-OCR's `140`), then one presented over one that is not, then a
 * labeled one, then the first.
 */
internal fun List<Detection>.onePerPlace(): List<Detection> {
    val kept = mutableListOf<Detection>()
    for (d in this) {
        val clash = kept.indexOfFirst { samePlace(it.box, d.box) || (it.value == d.value && sameRowNearby(it.box, d.box)) }
        if (clash < 0) {
            kept += d
        } else if (betterThan(d, kept[clash])) {
            kept[clash] = d
        }
    }
    return kept
}

private fun Detection.digits(): Int = value.count { it.isDigit() }

private fun betterThan(a: Detection, b: Detection): Boolean = when {
    a.digits() != b.digits() -> a.digits() > b.digits()
    (a.kind != null) != (b.kind != null) -> a.kind != null
    else -> a.basis == DetectionBasis.LABEL && b.basis != DetectionBasis.LABEL
}

/** On the same row (vertically overlapping by half the lower box) and at most one and a half box heights apart sideways. */
internal fun sameRowNearby(a: TextBox, b: TextBox): Boolean {
    val rows = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
    val height = minOf(a.height, b.height)
    if (height <= 0 || rows < height / 2) return false
    val gap = maxOf(a.left, b.left) - minOf(a.right, b.right)
    return gap <= maxOf(a.height, b.height) * MAX_GAP_HALF_HEIGHTS / 2
}

internal fun samePlace(a: TextBox, b: TextBox): Boolean {
    val w = minOf(a.right, b.right) - maxOf(a.left, b.left)
    val h = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
    if (w <= 0 || h <= 0) return false
    val smaller = minOf(a.width.toLong() * a.height, b.width.toLong() * b.height).coerceAtLeast(1)
    return w.toLong() * h >= smaller * ReadingThresholds.SAME_PLACE
}

/** The detections presented to the user: those with a kind. */
fun List<Detection>.candidates(): List<Detection> = filter { it.kind != null }

/** A unit of distance after a number: `km` and its misreads (`knm`, `ki`), or miles; never a speed (`km/h`, misread `Knh`). */
internal fun isDistanceUnit(word: String): Boolean {
    val w = word.lowercase()
    if (w.isEmpty() || '/' in w) return false
    if (w in setOf("mi", "mile", "miles")) return true
    return w.startsWith('k') && w.all { it.isLetter() } && !w.endsWith('h')
}

internal fun kindByMagnitude(value: Double, knownOdometer: Double?): ReadingKind? {
    if (knownOdometer == null) return if (value > ReadingThresholds.MAX_TRIP) ReadingKind.ODOMETER else ReadingKind.TRIP
    val margin = max(ReadingThresholds.MIN_ODOMETER_MARGIN, knownOdometer * ReadingThresholds.ODOMETER_MARGIN_FRACTION)
    return when {
        value >= knownOdometer && value <= knownOdometer + margin -> ReadingKind.ODOMETER
        value <= ReadingThresholds.MAX_TRIP && value < knownOdometer -> ReadingKind.TRIP
        else -> null
    }
}

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
private fun labelOf(photo: RecognizedPhoto, line: RecognizedLine, index: Int, box: TextBox, labels: Map<String, ReadingKind>): Pair<String, ReadingKind>? {
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

private fun isLoneT(text: String): Boolean = text.trim().equals("T", ignoreCase = true)

/** The vertical space between two boxes, zero when they overlap vertically. */
private fun verticalGap(a: TextBox, b: TextBox): Int = max(0, max(a.top - b.bottom, b.top - a.bottom))

/** The boxes overlap horizontally, or are apart by at most [number]'s width. */
private fun horizontallyNear(label: TextBox, number: TextBox): Boolean {
    val gap = max(label.left - number.right, number.left - label.right)
    return gap <= 0 || abs(gap) <= number.width
}
