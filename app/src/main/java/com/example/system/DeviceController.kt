package com.example.system

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.example.data.model.AppLanguage
import com.example.data.model.VoiceGender
import java.util.Locale

data class DevicePermissionReport(
    val microphoneGranted: Boolean,
    val cameraGranted: Boolean,
    val notificationsGranted: Boolean,
    val locationGranted: Boolean,
    val accessibilityEnabled: Boolean,
    val batteryOptimizedIgnored: Boolean
)

class DeviceController(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    fun initTts(onSpeakingChanged: (Boolean) -> Unit) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingChanged(false)
                }
            })
        }
    }

    fun speak(
        text: String,
        language: AppLanguage,
        gender: VoiceGender,
        speed: Float
    ) {
        if (!ttsReady) return
        val locale = when (language) {
            AppLanguage.EN -> Locale.US
            AppLanguage.UR -> Locale("ur", "PK")
            AppLanguage.HI -> Locale("hi", "IN")
        }
        tts?.language = locale
        tts?.setPitch(if (gender == VoiceGender.FEMALE) 1.08f else 0.85f)
        tts?.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova_utterance_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startVoiceRecognition(
        language: AppLanguage,
        onRmsChanged: (Float) -> Unit,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service unavailable on this device; use command bar or quick chips.")
            return
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                        onRmsChanged(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that, try again."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Voice input error ($error). Try again or type below."
                        }
                        onError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull().orEmpty()
                        if (best.isBlank()) {
                            onError("Didn't catch that, try again.")
                        } else {
                            onFinalResult(best)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val langTag = when (language) {
                AppLanguage.EN -> "en-US"
                AppLanguage.UR -> "ur-PK"
                AppLanguage.HI -> "hi-IN"
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Microphone unavailable.")
        }
    }

    fun stopVoiceRecognition() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun setFlashlight(enabled: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cameraManager.setTorchMode(cameraId, enabled)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setMediaVolumePercent(percent: Float) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (max * percent.coerceIn(0f, 1f)).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: Exception) {
        }
    }

    fun getMediaVolumePercent(): Float {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            cur.toFloat() / max.toFloat()
        } catch (_: Exception) {
            0.65f
        }
    }

    fun openSystemSettings(action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (action == Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        }
    }

    fun launchTargetAction(targetApp: String): Boolean {
        return try {
            val intent = when (targetApp.uppercase()) {
                "YOUTUBE" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                "SPOTIFY", "MUSIC" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
                "MAPS" -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                "PHONE" -> Intent(Intent.ACTION_DIAL)
                "WHATSAPP", "SMS" -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                "ALARM" -> Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, 7)
                    putExtra(AlarmClock.EXTRA_MINUTES, 0)
                }
                else -> return false
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun dialEmergency(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun persistBootPreference(startOnBoot: Boolean) {
        context.getSharedPreferences("nova_boot_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("start_on_boot", startOnBoot)
            .apply()
    }

    fun inspectPermissions(): DevicePermissionReport {
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val loc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            power?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
        return DevicePermissionReport(
            microphoneGranted = mic,
            cameraGranted = cam,
            notificationsGranted = notif,
            locationGranted = loc,
            accessibilityEnabled = NovaAccessibilityService.isConnected.value,
            batteryOptimizedIgnored = batteryIgnored
        )
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
