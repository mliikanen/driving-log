package com.mikonoma.drivinglog.vehicle.color

import com.mikonoma.drivinglog.vehicle.domain.Rgb
import com.mikonoma.drivinglog.vehicle.picture.PixelSamples
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The color taken from real photos: the JPEGs the Maestro flows use (`maestro/assets`). Each is cut to the centered square the crop screen starts with and reduced to
 * 128 pixels, as the app does, and the extracted color must be the color of the car (loosely: a hue, or "light and grey", not an exact value).
 */
class RealPhotoColorJvmTest {
    private val extractor = HistogramColorExtractor()

    private fun colorOf(name: String): Rgb {
        val file = File("../maestro/assets/$name")
        assertTrue(file.exists(), "${file.absolutePath} is missing")
        val photo = checkNotNull(ImageIO.read(file)) { "$name is not an image" }
        val side = minOf(photo.width, photo.height)
        val square = photo.getSubimage((photo.width - side) / 2, (photo.height - side) / 2, side, side)
        val small = BufferedImage(SAMPLE, SAMPLE, BufferedImage.TYPE_INT_ARGB)
        val graphics = small.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(square, 0, 0, SAMPLE, SAMPLE, null)
        graphics.dispose()
        val pixels = IntArray(SAMPLE * SAMPLE) { small.getRGB(it % SAMPLE, it / SAMPLE) }
        return checkNotNull(extractor.extract(PixelSamples(SAMPLE, SAMPLE, pixels))) { "$name gave no color" }
    }

    private fun Rgb.channels() = Triple((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)

    @Test
    fun theRedCarGivesRed() {
        val (r, g, b) = colorOf("photo-car-red.jpg").channels()

        assertTrue(r > g + 80 && r > b + 80, "r=$r g=$g b=$b")
    }

    @Test
    fun theBlueCarGivesBlue() {
        val (r, g, b) = colorOf("photo-car-blue.jpg").channels()

        assertTrue(b > r + 30 && b >= g, "r=$r g=$g b=$b")
    }

    @Test
    fun theGreyCarGivesALightGrey() {
        val (r, g, b) = colorOf("photo-car-grey.jpg").channels()

        assertTrue(maxOf(r, g, b) - minOf(r, g, b) < 50 && maxOf(r, g, b) > 110, "r=$r g=$g b=$b")
    }

    @Test
    fun theWhiteCarGivesWhite() {
        val (r, g, b) = colorOf("photo-car-white.jpg").channels()

        assertTrue(minOf(r, g, b) > 200, "r=$r g=$g b=$b")
    }

    private companion object {
        const val SAMPLE = 128
    }
}
