package com.mikonoma.drivinglog.vehicle.domain

import kotlin.jvm.JvmInline

/** A fuel amount in whole milliliters, the only form in which a refueling's amount is stored (`add-refueling-logging`). */
@JvmInline
value class Volume(val milliliters: Long) {
    init {
        require(milliliters >= 0) { "Volume cannot be negative" }
    }

    companion object {
        val ZERO = Volume(0)
    }
}
