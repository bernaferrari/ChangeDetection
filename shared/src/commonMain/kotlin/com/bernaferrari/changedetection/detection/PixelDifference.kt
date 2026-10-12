package com.bernaferrari.changedetection.detection

/**
 * One pixel-distance rule for decoded, unpremultiplied ARGB on every platform.
 * Tolerance is normalized to the full RGB distance; alpha participates with equal
 * channel weight, preserving the existing Android comparison convention.
 */
internal class PixelDifference(tolerance: Double) {
    init {
        require(tolerance in 0.0..1.0) { "Pixel tolerance must be between zero and one" }
    }

    private val limitSquared = tolerance * tolerance * 255 * 255 * 3

    fun changed(before: Int, after: Int): Boolean {
        var distanceSquared = 0
        for (shift in 0..24 step 8) {
            val delta = ((before ushr shift) and 255) - ((after ushr shift) and 255)
            distanceSquared += delta * delta
        }
        return distanceSquared > limitSquared
    }
}
