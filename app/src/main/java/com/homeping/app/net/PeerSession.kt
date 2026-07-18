package com.homeping.app.net

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One TCP connection through hello + mutual PIN auth.
 * After [onAuthenticated], the read loop keeps running for later ping messages.
 */
class PeerSession(
    private val socket: Socket,
    private val selfDeviceId: String,
    private val selfDisplayName: String,
    private val homePin: String,
    private val scope: CoroutineScope,
    private val onAuthenticated: (peerId: String, peerName: String) -> Unit,
    private val onClosed: (reason: String) -> Unit,
    private val onProtocolMessage: (ProtocolMessage) -> Unit = {},
) {
    private val closed = AtomicBoolean(false)
    private val selfNonce = PinAuth.newNonce()
    private var peerNonce: String? = null
    private var peerDeviceId: String? = null
    private var peerDisplayName: String = ""
    private var theirProofValid = false
    private var receivedAuthOk = false
    private var authAnnounced = false

    private val outbound = Channel<ProtocolMessage>(Channel.BUFFERED)
    private var readerJob: Job? = null
    private var writerJob: Job? = null

    val remoteHost: String
        get() = socket.inetAddress?.hostAddress ?: socket.inetAddress?.hostName.orEmpty()

    fun start() {
        socket.tcpNoDelay = true
        socket.keepAlive = true
        val input = BufferedInputStream(socket.getInputStream())
        val output = BufferedOutputStream(socket.getOutputStream())

        writerJob = scope.launch(Dispatchers.IO) {
            try {
                for (msg in outbound) {
                    FrameIo.writeMessage(output, msg)
                }
            } catch (e: Exception) {
                close("write failed: ${e.message}")
            }
        }

        readerJob = scope.launch(Dispatchers.IO) {
            try {
                send(
                    ProtocolMessage.Hello(
                        deviceId = selfDeviceId,
                        displayName = selfDisplayName,
                        protocolVersion = Protocol.VERSION,
                        nonce = selfNonce,
                    ),
                )
                while (!closed.get()) {
                    val msg = FrameIo.readMessage(input)
                    handle(msg)
                }
            } catch (e: Exception) {
                close("read failed: ${e.message}")
            }
        }
    }

    fun send(message: ProtocolMessage) {
        if (closed.get()) return
        outbound.trySend(message)
    }

    fun close(reason: String) {
        if (!closed.compareAndSet(false, true)) return
        Log.i(TAG, "close: $reason")
        outbound.close()
        readerJob?.cancel()
        writerJob?.cancel()
        try {
            socket.close()
        } catch (_: Exception) {
        }
        onClosed(reason)
    }

    private suspend fun handle(msg: ProtocolMessage) {
        when (msg) {
            is ProtocolMessage.Hello -> {
                peerDeviceId = msg.deviceId
                peerDisplayName = msg.displayName.ifBlank { msg.deviceId.take(8) }
                peerNonce = msg.nonce
                if (msg.deviceId == selfDeviceId) {
                    close("connected to self")
                    return
                }
                val proof = PinAuth.computeProof(
                    pin = homePin,
                    noncePeer = msg.nonce,
                    nonceSelf = selfNonce,
                    deviceIdSelf = selfDeviceId,
                )
                send(
                    ProtocolMessage.Auth(
                        deviceId = selfDeviceId,
                        proof = proof,
                    ),
                )
            }
            is ProtocolMessage.Auth -> {
                val remoteNonce = peerNonce
                if (remoteNonce == null) {
                    // Auth arrived before hello; stash is not supported — fail closed.
                    send(ProtocolMessage.AuthFail("hello required first"))
                    close("auth before hello")
                    return
                }
                if (msg.deviceId != peerDeviceId && peerDeviceId != null) {
                    send(ProtocolMessage.AuthFail("deviceId mismatch"))
                    close("deviceId mismatch")
                    return
                }
                peerDeviceId = msg.deviceId
                val ok = PinAuth.verifyProof(
                    pin = homePin,
                    nonceLocal = selfNonce,
                    nonceRemote = remoteNonce,
                    deviceIdRemote = msg.deviceId,
                    proof = msg.proof,
                )
                if (!ok) {
                    send(ProtocolMessage.AuthFail("bad pin proof"))
                    close("auth failed")
                    return
                }
                theirProofValid = true
                send(ProtocolMessage.AuthOk(deviceId = selfDeviceId))
                maybeAnnounceAuthenticated()
            }
            is ProtocolMessage.AuthOk -> {
                receivedAuthOk = true
                maybeAnnounceAuthenticated()
            }
            is ProtocolMessage.AuthFail -> {
                close("peer rejected auth: ${msg.reason}")
            }
            is ProtocolMessage.Presence,
            is ProtocolMessage.Ping,
            is ProtocolMessage.PingDelivered,
            is ProtocolMessage.PingAck,
            is ProtocolMessage.PingCancel,
            is ProtocolMessage.Unknown,
            -> {
                if (authAnnounced) {
                    onProtocolMessage(msg)
                }
            }
        }
    }

    fun isAuthenticated(): Boolean = authAnnounced

    private fun maybeAnnounceAuthenticated() {
        if (authAnnounced) return
        if (!theirProofValid || !receivedAuthOk) return
        val id = peerDeviceId ?: return
        authAnnounced = true
        Log.i(TAG, "authenticated with $peerDisplayName ($id)")
        onAuthenticated(id, peerDisplayName)
    }

    companion object {
        private const val TAG = "PeerSession"

        suspend fun connect(
            host: String,
            port: Int,
            selfDeviceId: String,
            selfDisplayName: String,
            homePin: String,
            scope: CoroutineScope,
            onAuthenticated: (peerId: String, peerName: String) -> Unit,
            onClosed: (reason: String) -> Unit,
            onProtocolMessage: (ProtocolMessage) -> Unit = {},
        ): PeerSession = withContext(Dispatchers.IO) {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            PeerSession(
                socket = socket,
                selfDeviceId = selfDeviceId,
                selfDisplayName = selfDisplayName,
                homePin = homePin,
                scope = scope,
                onAuthenticated = onAuthenticated,
                onClosed = onClosed,
                onProtocolMessage = onProtocolMessage,
            ).also { it.start() }
        }

        private const val CONNECT_TIMEOUT_MS = 5_000
    }
}
