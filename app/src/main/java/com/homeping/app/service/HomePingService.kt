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
import com.homeping.app.alert.PingAlerter
import com.homeping.app.data.PreferencesRepository
import com.homeping.app.discovery.NsdPeerDiscovery
import com.homeping.app.net.SessionManager
import com.homeping.app.net.SessionRegistry
import com.homeping.app.ping.PingHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service: ready notification, discovery, TCP session, and pings.
 */
class HomePingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var discovery: NsdPeerDiscovery? = null
    private var sessionManager: SessionManager? = null
    private var prefsJob: Job? = null
    private var lastDeviceId: String = ""
    private var lastDisplayName: String = ""
    private var lastPin: String = ""
    private lateinit var pingAlerter: PingAlerter

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        pingAlerter = PingAlerter(this)
        Log.i(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "stop requested")
                tearDownNetworking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SEND_PING -> {
                promoteToForeground()
                ensurePrefsWatching()
                val ok = sessionManager?.sendPing() == true
                Log.i(TAG, "send ping requested ok=$ok")
            }
            else -> {
                promoteToForeground()
                ensurePrefsWatching()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tearDownNetworking()
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
    }

    private fun ensurePrefsWatching() {
        if (prefsJob != null) return
        val repo = PreferencesRepository.getInstance(this)
        prefsJob = scope.launch {
            repo.preferences.collect { prefs ->
                if (!prefs.setupComplete || prefs.deviceId.isBlank() || prefs.homePin.isBlank()) {
                    tearDownNetworking()
                    lastDeviceId = ""
                    lastDisplayName = ""
                    lastPin = ""
                    return@collect
                }

                val identityChanged =
                    discovery == null ||
                        sessionManager == null ||
                        prefs.deviceId != lastDeviceId ||
                        prefs.homePin != lastPin

                if (identityChanged) {
                    tearDownNetworking()
                    discovery = NsdPeerDiscovery(
                        context = this@HomePingService,
                        selfDeviceId = prefs.deviceId,
                        displayName = prefs.displayName.ifBlank { "HomePing" },
                    ).also { it.start() }

                    sessionManager = SessionManager(
                        scope = scope,
                        onPaired = { peerId, peerName ->
                            repo.setPairedPeer(peerId, peerName)
                        },
                        onIncomingAlert = { pingId, fromName ->
                            pingAlerter.showIncoming(pingId, fromName)
                        },
                        onIncomingCleared = {
                            pingAlerter.clearAll()
                        },
                    ).also {
                        it.start(
                            selfDeviceId = prefs.deviceId,
                            selfDisplayName = prefs.displayName.ifBlank { "HomePing" },
                            homePin = prefs.homePin,
                            preferredPeerId = prefs.pairedPeerId,
                        )
                    }
                    lastDeviceId = prefs.deviceId
                    lastDisplayName = prefs.displayName
                    lastPin = prefs.homePin
                } else {
                    if (prefs.displayName != lastDisplayName) {
                        discovery?.updateDisplayName(prefs.displayName.ifBlank { "HomePing" })
                        lastDisplayName = prefs.displayName
                    }
                    sessionManager?.updateIdentity(
                        displayName = prefs.displayName.ifBlank { "HomePing" },
                        homePin = prefs.homePin,
                        preferredPeerId = prefs.pairedPeerId,
                    )
                }
            }
        }
    }

    private fun tearDownNetworking() {
        pingAlerter.clearAll()
        sessionManager?.stop()
        sessionManager = null
        discovery?.stop()
        discovery = null
        SessionRegistry.reset()
        PingHub.detach()
    }

    companion object {
        private const val TAG = "HomePingService"
        const val ACTION_STOP = "com.homeping.app.action.STOP_SERVICE"
        const val ACTION_SEND_PING = "com.homeping.app.action.SEND_PING"

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

        fun requestSendPing(context: Context) {
            val intent = Intent(context, HomePingService::class.java).apply {
                action = ACTION_SEND_PING
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun isPermissionReady(context: Context): Boolean {
            return NotificationPermission.hasPostNotifications(context)
        }
    }
}
