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
import com.homeping.app.data.PreferencesRepository
import com.homeping.app.discovery.NsdPeerDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps HomePing eligible to run while the user expects pings.
 * Owns the ready notification and LAN peer discovery (NSD).
 */
class HomePingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var discovery: NsdPeerDiscovery? = null
    private var prefsJob: Job? = null
    private var lastDeviceId: String = ""
    private var lastDisplayName: String = ""

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        Log.i(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "stop requested")
                tearDownDiscovery()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                promoteToForeground()
                ensureDiscoveryWatching()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tearDownDiscovery()
        prefsJob?.cancel()
        prefsJob = null
        scope.cancel()
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

    private fun ensureDiscoveryWatching() {
        if (prefsJob != null) return
        val repo = PreferencesRepository.getInstance(this)
        prefsJob = scope.launch {
            repo.preferences.collect { prefs ->
                if (!prefs.setupComplete || prefs.deviceId.isBlank()) {
                    tearDownDiscovery()
                    lastDeviceId = ""
                    lastDisplayName = ""
                    return@collect
                }
                if (discovery == null || prefs.deviceId != lastDeviceId) {
                    tearDownDiscovery()
                    discovery = NsdPeerDiscovery(
                        context = this@HomePingService,
                        selfDeviceId = prefs.deviceId,
                        displayName = prefs.displayName.ifBlank { "HomePing" },
                    ).also { it.start() }
                    lastDeviceId = prefs.deviceId
                    lastDisplayName = prefs.displayName
                } else if (prefs.displayName != lastDisplayName) {
                    discovery?.updateDisplayName(prefs.displayName.ifBlank { "HomePing" })
                    lastDisplayName = prefs.displayName
                }
            }
        }
    }

    private fun tearDownDiscovery() {
        discovery?.stop()
        discovery = null
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
