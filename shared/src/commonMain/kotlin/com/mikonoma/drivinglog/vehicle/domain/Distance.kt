package com.mikonoma.drivinglog.vehicle.domain

import kotlin.jvm.JvmInline

/** A distance in whole meters, the only form in which distances are stored. */
@JvmInline
value class Distance(val meters: Long) {
    init {
        require(meters >= 0) { "Distance cannot be negative" }
    }

    companion object {
        val ZERO = Distance(0)
    }
}
