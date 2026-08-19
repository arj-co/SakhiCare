package com.sakhicare.app.voice

import android.media.MediaRecorder

object MicConfig {
    const val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION
    const val SAMPLE_RATE_HZ = 16000
    const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
}
