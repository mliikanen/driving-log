package com.mikonoma.drivinglog.vehicle.picture

/** A codec that opens nothing. Stands in until the platform codecs exist; every photo then "cannot be opened". */
object UnavailableImageCodec : ImageCodec {
    override suspend fun decode(bytes: ByteArray): DecodedImage? = null
    override suspend fun encodeSquare(bytes: ByteArray, crop: CropRect, sides: PictureSides): EncodedPicture? = null
}
