package com.mikonoma.drivinglog.vehicle.ocr

/** A recognizer for tests: empty bytes are "not an image"; any other bytes read as [photo], which a test sets to the text it needs. */
class FakeTextRecognizer(var photo: RecognizedPhoto = RecognizedPhoto(1280, 720, emptyList()), override val isAvailable: Boolean = true) : TextRecognizer {
    val recognized = mutableListOf<ByteArray>()

    val frames = mutableListOf<com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage>()

    override suspend fun recognize(frame: com.mikonoma.drivinglog.vehicle.ocr.ppocr.RgbImage): RecognizedPhoto? {
        frames += frame
        return photo
    }

    override suspend fun recognize(bytes: ByteArray): RecognizedPhoto? {
        if (bytes.isEmpty()) return null
        recognized += bytes
        return photo
    }
}
