package com.homeping.app.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Advertises this device and discovers other HomePing instances via Android NSD (mDNS).
 *
 * Call [start] / [stop] from the process that owns the foreground service.
 * Only one resolve runs at a time (platform limitation on many API levels).
 */
class NsdPeerDiscovery(
    context: Context,
    private val selfDeviceId: String,
    private var displayName: String,
    private val port: Int = DiscoveryConstants.CONTROL_PORT,
) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(NsdManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registeredInfo: NsdServiceInfo? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolveInFlight = false
    private val started = AtomicBoolean(false)

    fun start() {
        if (started.getAndSet(true)) return
        if (nsdManager == null) {
            Log.e(TAG, "NsdManager unavailable")
            started.set(false)
            return
        }
        if (selfDeviceId.isBlank()) {
            Log.e(TAG, "Cannot advertise without deviceId")
            started.set(false)
            return
        }
        acquireMulticastLock()
        registerService()
        startDiscovery()
        Log.i(TAG, "started as '$displayName' (${selfDeviceId.take(8)})")
    }

    fun stop() {
        if (!started.getAndSet(false)) return
        mainHandler.post {
            stopDiscoveryInternal()
            unregisterServiceInternal()
            resolveQueue.clear()
            resolveInFlight = false
            releaseMulticastLock()
            PeerDirectory.clear()
            Log.i(TAG, "stopped")
        }
    }

    /** Re-advertise after the user renames this phone. */
    fun updateDisplayName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == displayName) return
        displayName = trimmed
        if (!started.get()) return
        mainHandler.post {
            unregisterServiceInternal()
            registerService()
        }
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = appContext.getSystemService(WifiManager::class.java) ?: return
            multicastLock = wifi.createMulticastLock(DiscoveryConstants.MULTICAST_LOCK_TAG).apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "MulticastLock failed: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {
        } finally {
            multicastLock = null
        }
    }

    private fun registerService() {
        val manager = nsdManager ?: return
        val listenPort = port
        val info = NsdServiceInfo().apply {
            serviceName = ServiceNameUtil.build(displayName, selfDeviceId)
            serviceType = DiscoveryConstants.SERVICE_TYPE
            setPort(listenPort)
            setAttribute(DiscoveryConstants.ATTR_DEVICE_ID, selfDeviceId)
            setAttribute(DiscoveryConstants.ATTR_NAME, displayName.take(64))
            setAttribute(DiscoveryConstants.ATTR_VER, DiscoveryConstants.PROTOCOL_VERSION)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "register failed code=$errorCode name=${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "unregister failed code=$errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                registeredInfo = serviceInfo
                Log.i(TAG, "registered ${serviceInfo.serviceName}")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "unregistered ${serviceInfo.serviceName}")
                if (registeredInfo?.serviceName == serviceInfo.serviceName) {
                    registeredInfo = null
                }
            }
        }
        registrationListener = listener
        try {
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "registerService threw", e)
        }
    }

    private fun unregisterServiceInternal() {
        val manager = nsdManager ?: return
        val listener = registrationListener ?: return
        try {
            manager.unregisterService(listener)
        } catch (e: Exception) {
            Log.w(TAG, "unregisterService: ${e.message}")
        }
        registrationListener = null
        registeredInfo = null
    }

    private fun startDiscovery() {
        val manager = nsdManager ?: return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "discover start failed code=$errorCode")
                try {
                    manager.stopServiceDiscovery(this)
                } catch (_: Exception) {
                }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "discover stop failed code=$errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.i(TAG, "discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val type = serviceInfo.serviceType.orEmpty()
                if (!type.contains("homeping", ignoreCase = true)) {
                    return
                }
                val ownName = registeredInfo?.serviceName
                if (ownName != null && serviceInfo.serviceName == ownName) {
                    return
                }
                Log.d(TAG, "found ${serviceInfo.serviceName} type=$type")
                enqueueResolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "lost ${serviceInfo.serviceName}")
                PeerDirectory.removeByServiceName(serviceInfo.serviceName)
            }
        }
        discoveryListener = listener
        try {
            manager.discoverServices(
                DiscoveryConstants.SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                listener,
            )
        } catch (e: Exception) {
            Log.e(TAG, "discoverServices threw", e)
        }
    }

    private fun stopDiscoveryInternal() {
        val manager = nsdManager ?: return
        val listener = discoveryListener ?: return
        try {
            manager.stopServiceDiscovery(listener)
        } catch (e: Exception) {
            Log.w(TAG, "stopServiceDiscovery: ${e.message}")
        }
        discoveryListener = null
    }

    private fun enqueueResolve(info: NsdServiceInfo) {
        mainHandler.post {
            resolveQueue.addLast(info)
            pumpResolveQueue()
        }
    }

    private fun pumpResolveQueue() {
        if (resolveInFlight) return
        val next = resolveQueue.pollFirst() ?: return
        resolveInFlight = true
        val manager = nsdManager
        if (manager == null) {
            resolveInFlight = false
            return
        }
        try {
            manager.resolveService(
                next,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "resolve failed ${serviceInfo.serviceName} code=$errorCode")
                        mainHandler.post {
                            resolveInFlight = false
                            pumpResolveQueue()
                        }
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        handleResolved(serviceInfo)
                        mainHandler.post {
                            resolveInFlight = false
                            pumpResolveQueue()
                        }
                    }
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "resolveService threw", e)
            resolveInFlight = false
            pumpResolveQueue()
        }
    }

    private fun handleResolved(serviceInfo: NsdServiceInfo) {
        val attrs = serviceInfo.attributes.orEmpty()
        val deviceId = attrString(attrs, DiscoveryConstants.ATTR_DEVICE_ID)
            .ifBlank { "unknown-${serviceInfo.serviceName}" }
        if (deviceId == selfDeviceId) {
            Log.d(TAG, "skip self resolve")
            return
        }
        val name = attrString(attrs, DiscoveryConstants.ATTR_NAME)
            .ifBlank { serviceInfo.serviceName }
        val ver = attrString(attrs, DiscoveryConstants.ATTR_VER).toIntOrNull() ?: 1
        val host = serviceInfo.host?.hostAddress
            ?: serviceInfo.host?.hostName
            ?: return
        val peer = DiscoveredPeer(
            deviceId = deviceId,
            displayName = name,
            host = host,
            port = if (serviceInfo.port > 0) serviceInfo.port else port,
            serviceName = serviceInfo.serviceName,
            protocolVersion = ver,
        )
        PeerDirectory.upsert(serviceInfo.serviceName, peer)
        Log.i(TAG, "peer online: ${peer.displayName} @ ${peer.host}:${peer.port}")
    }

    private fun attrString(attrs: Map<String, ByteArray?>, key: String): String {
        val bytes = attrs[key] ?: return ""
        return try {
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private const val TAG = "NsdPeerDiscovery"
    }
}
