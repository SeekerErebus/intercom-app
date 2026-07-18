package com.homeping.app.net

import android.util.Log
import com.homeping.app.discovery.DiscoveredPeer
import com.homeping.app.discovery.DiscoveryConstants
import com.homeping.app.discovery.PeerDirectory
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Listens for inbound HomePing TCP sessions and dials discovered peers.
 * Two-device policy: the lexicographically smaller deviceId dials to avoid
 * duplicate connections; the larger waits for inbound.
 */
class SessionManager(
    private val scope: CoroutineScope,
    private val onPaired: suspend (peerId: String, peerName: String) -> Unit,
) {
    private val running = AtomicBoolean(false)
    private val sessionMutex = Mutex()

    private var selfDeviceId: String = ""
    private var selfDisplayName: String = ""
    private var homePin: String = ""
    private var preferredPeerId: String = ""

    private var serverJob: Job? = null
    private var peerWatchJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var activeSession: PeerSession? = null

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    fun start(
        selfDeviceId: String,
        selfDisplayName: String,
        homePin: String,
        preferredPeerId: String = "",
    ) {
        this.selfDeviceId = selfDeviceId
        this.selfDisplayName = selfDisplayName
        this.homePin = homePin
        this.preferredPeerId = preferredPeerId
        if (!running.compareAndSet(false, true)) {
            return
        }
        publish(ConnectionStatus.Listening)
        startServer()
        watchPeers()
        Log.i(TAG, "SessionManager started as ${selfDeviceId.take(8)}")
    }

    fun updateIdentity(displayName: String, homePin: String, preferredPeerId: String) {
        selfDisplayName = displayName
        this.homePin = homePin
        this.preferredPeerId = preferredPeerId
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        peerWatchJob?.cancel()
        peerWatchJob = null
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        scope.launch {
            sessionMutex.withLock {
                activeSession?.close("manager stop")
                activeSession = null
            }
        }
        publish(ConnectionStatus.Idle)
        Log.i(TAG, "SessionManager stopped")
    }

    private fun publish(status: ConnectionStatus) {
        _status.value = status
        SessionRegistry.publish(status)
    }

    private fun startServer() {
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(DiscoveryConstants.CONTROL_PORT).apply {
                    reuseAddress = true
                }
                serverSocket = server
                Log.i(TAG, "listening on ${DiscoveryConstants.CONTROL_PORT}")
                while (isActive && running.get()) {
                    val socket = try {
                        server.accept()
                    } catch (e: Exception) {
                        if (running.get()) Log.w(TAG, "accept: ${e.message}")
                        break
                    }
                    Log.i(TAG, "inbound from ${socket.inetAddress?.hostAddress}")
                    handleInbound(socket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "server failed: ${e.message}")
                if (running.get()) {
                    publish(ConnectionStatus.Failed(null, "Listen failed: ${e.message}"))
                }
            }
        }
    }

    private fun watchPeers() {
        peerWatchJob = scope.launch {
            while (isActive && running.get()) {
                maybeDial()
                delay(PEER_POLL_MS)
            }
        }
    }

    private suspend fun maybeDial() {
        if (!running.get()) return
        sessionMutex.withLock {
            if (activeSession != null) return
        }
        val peers = PeerDirectory.peers.value
        if (peers.isEmpty()) {
            val current = _status.value
            if (current is ConnectionStatus.Listening ||
                current is ConnectionStatus.Idle ||
                current is ConnectionStatus.Failed
            ) {
                publish(ConnectionStatus.Listening)
            }
            return
        }

        val candidate = selectDialCandidate(peers) ?: return
        // Lower deviceId dials; higher waits for inbound.
        if (selfDeviceId >= candidate.deviceId) {
            return
        }
        dial(candidate)
    }

    private fun selectDialCandidate(peers: List<DiscoveredPeer>): DiscoveredPeer? {
        if (preferredPeerId.isNotBlank()) {
            peers.firstOrNull { it.deviceId == preferredPeerId }?.let { return it }
        }
        return PeerDirectory.selectPrimary(preferredPeerId) ?: peers.firstOrNull()
    }

    private suspend fun dial(peer: DiscoveredPeer) {
        sessionMutex.withLock {
            if (activeSession != null) return
            publish(ConnectionStatus.Connecting(peer.displayName, peer.host))
        }
        try {
            Log.i(TAG, "dialing ${peer.displayName} @ ${peer.host}:${peer.port}")
            val session = PeerSession.connect(
                host = peer.host,
                port = if (peer.port > 0) peer.port else DiscoveryConstants.CONTROL_PORT,
                selfDeviceId = selfDeviceId,
                selfDisplayName = selfDisplayName,
                homePin = homePin,
                scope = scope,
                onAuthenticated = { id, name -> onSessionAuthenticated(id, name) },
                onClosed = { reason -> onSessionClosed(reason) },
            )
            sessionMutex.withLock {
                if (activeSession != null) {
                    session.close("already have session")
                    return
                }
                activeSession = session
                publish(ConnectionStatus.Handshaking(peer.displayName, peer.host))
            }
        } catch (e: Exception) {
            Log.w(TAG, "dial failed: ${e.message}")
            publish(ConnectionStatus.Failed(peer.displayName, e.message ?: "connect failed"))
            delay(RECONNECT_BACKOFF_MS)
        }
    }

    private suspend fun handleInbound(socket: Socket) {
        sessionMutex.withLock {
            if (activeSession != null) {
                Log.i(TAG, "reject inbound; session already active")
                try {
                    socket.close()
                } catch (_: Exception) {
                }
                return
            }
            val host = socket.inetAddress?.hostAddress ?: "?"
            publish(ConnectionStatus.Handshaking(peerName = "Peer", host = host))
            val session = PeerSession(
                socket = socket,
                selfDeviceId = selfDeviceId,
                selfDisplayName = selfDisplayName,
                homePin = homePin,
                scope = scope,
                onAuthenticated = { id, name -> onSessionAuthenticated(id, name) },
                onClosed = { reason -> onSessionClosed(reason) },
            )
            activeSession = session
            session.start()
        }
    }

    private fun onSessionAuthenticated(peerId: String, peerName: String) {
        preferredPeerId = peerId
        val host = activeSession?.remoteHost.orEmpty()
        publish(ConnectionStatus.Authenticated(peerId, peerName, host))
        scope.launch {
            try {
                onPaired(peerId, peerName)
            } catch (e: Exception) {
                Log.w(TAG, "onPaired failed: ${e.message}")
            }
        }
    }

    private fun onSessionClosed(reason: String) {
        scope.launch {
            sessionMutex.withLock {
                activeSession = null
            }
            if (running.get()) {
                publish(ConnectionStatus.Failed(null, reason))
                delay(RECONNECT_BACKOFF_MS)
                if (running.get() && activeSession == null) {
                    publish(ConnectionStatus.Listening)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SessionManager"
        private const val PEER_POLL_MS = 2_000L
        private const val RECONNECT_BACKOFF_MS = 3_000L
    }
}
