package com.mikonoma.drivinglog.vehicle.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.picture.PixelSamples

/** An extractor for processor tests: gives [color] (null for "the photo has no color") and remembers what it was asked about. */
class FakeColorExtractor(var color: Rgb? = Rgb(0xE53935)) : ColorExtractor {
    val extracted = mutableListOf<PixelSamples>()

    override fun extract(samples: PixelSamples): Rgb? {
        extracted += samples
        return color
    }
}
