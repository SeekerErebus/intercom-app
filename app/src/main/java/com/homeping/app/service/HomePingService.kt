package com.homeping.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.homeping.app.alert.HomePingNotifications
import com.homeping.app.alert.NotificationChannels

/**
 * Keeps HomePing eligible to run while the user expects pings.
 * Discovery and TCP listeners attach here in later PRs; this PR only owns
 * the foreground lifecycle and ongoing "ready" notification.
 */
class HomePingService : Service() {

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        Log.i(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "stop requested")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                promoteToForeground()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val notification = HomePingNotifications.serviceReady(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                HomePingNotifications.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(HomePingNotifications.SERVICE_NOTIFICATION_ID, notification)
        }
        Log.i(TAG, "foreground started")
    }

    companion object {
        private const val TAG = "HomePingService"
        const val ACTION_STOP = "com.homeping.app.action.STOP_SERVICE"

        fun start(context: Context) {
            NotificationChannels.ensureCreated(context)
            val intent = Intent(context, HomePingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HomePingService::class.java).apply {
                action = ACTION_STOP
            }
            // Prefer explicit stopSelf path via startCommand; also stopService as backup.
            try {
                context.startService(intent)
            } catch (_: Exception) {
                context.stopService(Intent(context, HomePingService::class.java))
            }
        }

        fun isPermissionReady(context: Context): Boolean {
            return NotificationPermission.hasPostNotifications(context)
        }
    }
}
