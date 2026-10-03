package com.mikonoma.drivinglog.vehicle.picture

private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~/"
private const val HEX_DIGITS = "0123456789ABCDEF"
private const val NIBBLE_BITS = 4

/**
 * The `file://` URI of an absolute path: everything outside the unreserved characters (and the `/` between segments) is
 * percent-encoded as UTF-8, so a directory such as iOS's "Application Support" (a space in its name) is a valid URI.
 */
fun fileUri(absolutePath: String): String {
    require(absolutePath.startsWith("/")) { "Not an absolute path: $absolutePath" }
    val encoded = StringBuilder("file://")
    for (byte in absolutePath.encodeToByteArray()) {
        val c = byte.toInt().toChar()
        if (byte >= 0 && c in UNRESERVED) {
            encoded.append(c)
        } else {
            val v = byte.toInt() and 0xFF
            encoded.append('%').append(HEX_DIGITS[v shr NIBBLE_BITS]).append(HEX_DIGITS[v and 0x0F])
        }
    }
    return encoded.toString()
}
