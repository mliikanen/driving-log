package com.mikonoma.drivinglog.vehicle.domain

import kotlinx.datetime.TimeZone

/** The device's current time zone. Read on every call so a change is picked up without restarting; tests fake it. */
interface DeviceTimeZone {
    fun current(): TimeZone
}

class SystemDeviceTimeZone : DeviceTimeZone {
    override fun current(): TimeZone = TimeZone.currentSystemDefault()
}
