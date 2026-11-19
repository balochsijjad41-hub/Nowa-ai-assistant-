package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class NovaBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("nova_boot_prefs", Context.MODE_PRIVATE)
            val startOnBoot = prefs.getBoolean("start_on_boot", true)
            if (startOnBoot) {
                val serviceIntent = Intent(context, NovaBackgroundService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
