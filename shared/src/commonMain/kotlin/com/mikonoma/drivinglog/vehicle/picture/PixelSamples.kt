package com.mikonoma.drivinglog.vehicle.picture

/** A photo reduced to a bounded array of pixels, row by row, each an ARGB int: what a color is extracted from. Holds no encoded bytes. */
class PixelSamples(val width: Int, val height: Int, val argb: IntArray) {
    init {
        require(width > 0 && height > 0) { "A sample has a size" }
        require(argb.size == width * height) { "${argb.size} pixels do not fill $width x $height" }
    }
}
