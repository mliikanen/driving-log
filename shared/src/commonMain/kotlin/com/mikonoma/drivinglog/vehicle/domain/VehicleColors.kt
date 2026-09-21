package com.mikonoma.drivinglog.vehicle.domain

/** A named preset of the vehicle color choice. The name is also the swatch's accessibility label. */
data class VehicleColorPreset(val name: String, val color: Rgb)

/**
 * The colors a vehicle can be given from the picker: [default] (the application's main theme color, which every vehicle starts with) and the
 * [presets], chosen as good bases for the palettes derived from a vehicle's color. Fuel Gauge Gold and Road Trip Emerald are left out on purpose:
 * they mean fuel and distance in the domain colors, and a vehicle color that looks the same would blur that.
 */
object VehicleColors {

    /**
     * The color of a vehicle that has not been given another one: Oil Slick Blue, the primary of the light theme (a test ties it to the theme). The schema's
     * column default is this same value as text, since a migration cannot call Kotlin (a migration test ties the two).
     */
    val default: Rgb = Rgb(0x203A43)

    /** The twelve presets, in the order they are offered; the first is the [default]. */
    val presets: List<VehicleColorPreset> = listOf(
        VehicleColorPreset("Petroleum", default),
        VehicleColorPreset("Teal", Rgb(0x00796B)),
        VehicleColorPreset("Green", Rgb(0x43A047)),
        VehicleColorPreset("Blue", Rgb(0x1E88E5)),
        VehicleColorPreset("Indigo", Rgb(0x3949AB)),
        VehicleColorPreset("Purple", Rgb(0x8E24AA)),
        VehicleColorPreset("Magenta", Rgb(0xD81B60)),
        VehicleColorPreset("Red", Rgb(0xE53935)),
        VehicleColorPreset("Orange", Rgb(0xFB8C00)),
        VehicleColorPreset("Brown", Rgb(0x6D4C41)),
        VehicleColorPreset("Graphite", Rgb(0x455A64)),
        VehicleColorPreset("Silver", Rgb(0x9E9E9E)),
    )

    /** The preset of [color], or null when it is not one. */
    fun presetOf(color: Rgb): VehicleColorPreset? = presets.firstOrNull { it.color == color }
}
