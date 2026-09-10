package com.sakhicare.app.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

enum class AudioRecordState {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED,
    PLAYING
}

/**
 * Audio Recording and Playback Manager for SakhiCare Point-of-Care Encounter Notes.
 *
 * Saves MPEG-4 AAC audio into app-private internal storage (`filesDir/voice_notes/`).
 * Manages recorder state, playback, recording timer, and SHA-256 checksum calculation.
 */
class AudioRecordManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecordManager"
        private const val AUDIO_DIR = "voice_notes"

        fun computeSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _recordState = MutableStateFlow(AudioRecordState.IDLE)
    val recordState: StateFlow<AudioRecordState> = _recordState.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _currentAudioFile = MutableStateFlow<File?>(null)
    val currentAudioFile: StateFlow<File?> = _currentAudioFile.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    private fun getAudioDirectory(): File {
        val dir = File(context.filesDir, AUDIO_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @Synchronized
    fun startRecording(caseId: String): Boolean {
        try {
            stopPlayback()
            releaseRecorder()

            val dir = getAudioDirectory()
            val timestamp = System.currentTimeMillis()
            val safeCaseId = caseId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val outputFile = File(dir, "case_${safeCaseId}_$timestamp.m4a")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _currentAudioFile.value = outputFile
            _durationSeconds.value = 0
            _recordState.value = AudioRecordState.RECORDING

            startTimer()
            Log.i(TAG, "Recording started -> ${outputFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseRecorder()
            _recordState.value = AudioRecordState.IDLE
            return false
        }
    }

    @Synchronized
    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _recordState.value == AudioRecordState.RECORDING) {
            try {
                mediaRecorder?.pause()
                _recordState.value = AudioRecordState.PAUSED
                stopTimer()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause recording", e)
            }
        }
    }

    @Synchronized
    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _recordState.value == AudioRecordState.PAUSED) {
            try {
                mediaRecorder?.resume()
                _recordState.value = AudioRecordState.RECORDING
                startTimer()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume recording", e)
            }
        }
    }

    @Synchronized
    fun stopRecording(): File? {
        if (_recordState.value == AudioRecordState.RECORDING || _recordState.value == AudioRecordState.PAUSED) {
            try {
                mediaRecorder?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recorder", e)
            } finally {
                releaseRecorder()
                stopTimer()
                _recordState.value = AudioRecordState.STOPPED
            }
        }
        return _currentAudioFile.value
    }

    @Synchronized
    fun startPlayback(onComplete: () -> Unit = {}) {
        val file = _currentAudioFile.value ?: return
        if (!file.exists()) return

        stopPlayback()
        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _recordState.value = AudioRecordState.STOPPED
                    onComplete()
                }
                start()
            }
            mediaPlayer = player
            _recordState.value = AudioRecordState.PLAYING
        } catch (e: Exception) {
            Log.e(TAG, "Playback failed", e)
            stopPlayback()
        }
    }

    @Synchronized
    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping playback", e)
        } finally {
            mediaPlayer = null
            if (_recordState.value == AudioRecordState.PLAYING) {
                _recordState.value = AudioRecordState.STOPPED
            }
        }
    }

    @Synchronized
    fun deleteRecording() {
        stopPlayback()
        stopRecording()
        val file = _currentAudioFile.value
        if (file != null && file.exists()) {
            file.delete()
        }
        _currentAudioFile.value = null
        _durationSeconds.value = 0
        _recordState.value = AudioRecordState.IDLE
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _recordState.value == AudioRecordState.RECORDING) {
                delay(1000)
                _durationSeconds.value += 1
                try {
                    _amplitude.value = mediaRecorder?.maxAmplitude ?: 0
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing recorder", e)
        } finally {
            mediaRecorder = null
        }
    }

    fun cleanUp() {
        stopRecording()
        stopPlayback()
        stopTimer()
    }
}
