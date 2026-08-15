package com.sakhicare.app.voice

object AudioNoiseFilter {
    fun isVoiceActivityDetected(buffer: ShortArray, threshold: Short = 500): Boolean {
        for (sample in buffer) {
            if (kotlin.math.abs(sample.toInt()) > threshold) return true
        }
        return false
    }

    fun calculateEnergyLevel(buffer: ShortArray): Double {
        var sum = 0.0
        for (s in buffer) sum += s * s
        return kotlin.math.sqrt(sum / buffer.size)
    }
}
