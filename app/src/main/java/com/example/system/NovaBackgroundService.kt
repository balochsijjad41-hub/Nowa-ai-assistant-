package com.example.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NovaServiceBridge {
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _micPaused = MutableStateFlow(false)
    val micPaused: StateFlow<Boolean> = _micPaused.asStateFlow()

    private val _speakTriggerCount = MutableStateFlow(0)
    val speakTriggerCount: StateFlow<Int> = _speakTriggerCount.asStateFlow()

    fun setRunning(running: Boolean) {
        _serviceRunning.value = running
    }

    fun toggleMicPause() {
        _micPaused.value = !_micPaused.value
    }

    fun setMicPaused(paused: Boolean) {
        _micPaused.value = paused
    }

    fun triggerSpeakFromNotification() {
        _speakTriggerCount.value += 1
    }
}

class NovaBackgroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_MIC -> {
                NovaServiceBridge.toggleMicPause()
                updateNotification()
            }
            ACTION_SPEAK -> {
                NovaServiceBridge.triggerSpeakFromNotification()
                val launch = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(EXTRA_START_LISTENING, true)
                }
                startActivity(launch)
            }
            ACTION_STOP -> {
                NovaServiceBridge.setRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                NovaServiceBridge.setRunning(true)
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        NovaServiceBridge.setRunning(false)
        super.onDestroy()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val micPaused = NovaServiceBridge.micPaused.value
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val micPending = PendingIntent.getService(
            this,
            1,
            Intent(this, NovaBackgroundService::class.java).apply { action = ACTION_TOGGLE_MIC },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val speakPending = PendingIntent.getService(
            this,
            2,
            Intent(this, NovaBackgroundService::class.java).apply { action = ACTION_SPEAK },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPending = PendingIntent.getService(
            this,
            3,
            Intent(this, NovaBackgroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val statusText = if (micPaused) {
            "Standby • Wake-word mic muted"
        } else {
            "Always Listening • Say \"Hey Nova\""
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nova_tile)
            .setContentTitle("Nova Voice Engine")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, if (micPaused) "Resume Mic" else "Mute Mic", micPending)
            .addAction(0, "Speak", speakPending)
            .addAction(0, "Stop", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nova Always-On Voice Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent status and quick controls for Nova hands-free assistant"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "nova_voice_engine_channel"
        const val NOTIFICATION_ID = 1042
        const val ACTION_TOGGLE_MIC = "com.example.nova.ACTION_TOGGLE_MIC"
        const val ACTION_SPEAK = "com.example.nova.ACTION_SPEAK"
        const val ACTION_STOP = "com.example.nova.ACTION_STOP"
        const val EXTRA_START_LISTENING = "extra_start_listening"

        fun start(context: Context) {
            val intent = Intent(context, NovaBackgroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NovaBackgroundService::class.java))
            NovaServiceBridge.setRunning(false)
        }
    }
}
