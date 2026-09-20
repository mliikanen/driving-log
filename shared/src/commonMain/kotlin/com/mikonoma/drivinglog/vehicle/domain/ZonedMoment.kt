package com.mikonoma.drivinglog.vehicle.domain

import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

/**
 * The time zone a moment was entered in: the IANA [id] and that zone's UTC offset at the moment. The offset is what makes
 * the shown wall-clock time exact on every platform, whatever the device's time zone database says now.
 */
data class EventZone(val id: String, val offsetSeconds: Int)

/**
 * A moment a user entered or sees: the [instant] (which all ordering and derivation use) and the [zone] it was entered in.
 * [zone] is null only for events written before time zones were stored; those are shown in the device's zone.
 */
data class ZonedMoment(val instant: Instant, val zone: EventZone? = null) {

    /** The wall-clock date and time as it was entered: the instant shifted by the stored offset. Null without a zone. */
    val localDateTime: LocalDateTime?
        get() = zone?.let { (instant + it.offsetSeconds.seconds).toLocalDateTime(TimeZone.UTC) }

    companion object {
        fun of(instant: Instant, timeZone: TimeZone) =
            ZonedMoment(instant, EventZone(timeZone.id, timeZone.offsetAt(instant).totalSeconds))
    }
}
