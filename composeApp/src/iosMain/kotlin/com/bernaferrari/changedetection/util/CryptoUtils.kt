package com.bernaferrari.changedetection.util

actual object CryptoUtils {
    actual fun sha256(bytes: ByteArray): String {
        // Proper SHA-256 for iOS typically requires CommonCrypto C-interop setup
        // which requires a .def file in the shared module.
        // For now, using a stable 64-bit hash as a placeholder until cinterop is configured.
        var h = 1125899906842597L // prime
        for (b in bytes) {
            h = 31 * h + b.toInt().and(0xFF)
        }
        return h.toString(16)
    }
}
