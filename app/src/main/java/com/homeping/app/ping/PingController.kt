package com.homeping.app.ping

import android.util.Log
import com.homeping.app.net.PingResponse
import com.homeping.app.net.ProtocolMessage
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ping state machine for one authenticated peer link.
 * New pings supersede in-flight ones (design: prefer supersede).
 */
class PingController(
    private val scope: CoroutineScope,
    private val selfDeviceId: String,
    private val selfDisplayName: String,
    private val send: (ProtocolMessage) -> Boolean,
    private val onIncomingAlert: (pingId: String, fromName: String) -> Unit,
    private val onIncomingCleared: (pingId: String) -> Unit,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<PingUiState>(PingUiState.Idle)
    val state: StateFlow<PingUiState> = _state.asStateFlow()

    private var outgoingTimeoutJob: Job? = null
    private var incomingTimeoutJob: Job? = null
    private var activeOutgoingId: String? = null
    private var activeIncomingId: String? = null
    private var lastPeerName: String = "Peer"

    fun updatePeerName(name: String) {
        if (name.isNotBlank()) lastPeerName = name
    }

    fun sendPing(peerName: String): Boolean {
        val name = peerName.ifBlank { lastPeerName }
        lastPeerName = name
        val pingId = UUID.randomUUID().toString()
        val msg = ProtocolMessage.Ping(
            pingId = pingId,
            fromDeviceId = selfDeviceId,
            fromName = selfDisplayName,
            timestampMs = System.currentTimeMillis(),
        )
        if (!send(msg)) {
            _state.value = PingUiState.OutgoingResult(name, OutgoingOutcome.Failed)
            return false
        }
        cancelOutgoingTimeout()
        clearIncomingLocal(reasonCancelRemote = false)
        activeOutgoingId = pingId
        _state.value = PingUiState.OutgoingRinging(pingId, name)
        outgoingTimeoutJob = scope.launch {
            delay(PING_TIMEOUT_MS)
            mutex.withLock {
                if (activeOutgoingId == pingId) {
                    activeOutgoingId = null
                    _state.value = PingUiState.OutgoingResult(name, OutgoingOutcome.Timeout)
                }
            }
        }
        Log.i(TAG, "sent ping $pingId to $name")
        return true
    }

    fun cancelOutgoing() {
        scope.launch {
            mutex.withLock {
                val id = activeOutgoingId ?: return@withLock
                send(ProtocolMessage.PingCancel(id))
                activeOutgoingId = null
                cancelOutgoingTimeout()
                _state.value = PingUiState.OutgoingResult(lastPeerName, OutgoingOutcome.Cancelled)
            }
        }
    }

    fun respondIncoming(pingId: String, response: PingResponse) {
        scope.launch {
            mutex.withLock {
                if (activeIncomingId != null && activeIncomingId != pingId) {
                    // Stale action from an older notification.
                    return@withLock
                }
                if (activeIncomingId == null && _state.value !is PingUiState.IncomingAlert) {
                    // Allow respond if state matches ping id.
                    val incoming = _state.value as? PingUiState.IncomingAlert
                    if (incoming == null || incoming.pingId != pingId) return@withLock
                }
                send(ProtocolMessage.PingAck(pingId = pingId, response = response))
                finishIncoming(pingId)
            }
        }
    }

    fun handleRemote(message: ProtocolMessage) {
        scope.launch {
            when (message) {
                is ProtocolMessage.Ping -> onRemotePing(message)
                is ProtocolMessage.PingDelivered -> {
                    // Optional; no UI change required for v1.
                }
                is ProtocolMessage.PingAck -> onRemoteAck(message)
                is ProtocolMessage.PingCancel -> onRemoteCancel(message)
                else -> Unit
            }
        }
    }

    fun reset() {
        cancelOutgoingTimeout()
        cancelIncomingTimeout()
        val incomingId = activeIncomingId
        activeOutgoingId = null
        activeIncomingId = null
        _state.value = PingUiState.Idle
        if (incomingId != null) {
            onIncomingCleared(incomingId)
        }
    }

    private suspend fun onRemotePing(message: ProtocolMessage.Ping) {
        mutex.withLock {
            // Supersede any previous incoming alert.
            val previous = activeIncomingId
            if (previous != null && previous != message.pingId) {
                onIncomingCleared(previous)
            }
            // If we were ringing outbound, that session is independent; keep outbound.
            activeIncomingId = message.pingId
            val fromName = message.fromName.ifBlank { lastPeerName }
            _state.value = PingUiState.IncomingAlert(message.pingId, fromName)
            send(ProtocolMessage.PingDelivered(message.pingId))
            onIncomingAlert(message.pingId, fromName)
            cancelIncomingTimeout()
            incomingTimeoutJob = scope.launch {
                delay(PING_TIMEOUT_MS)
                mutex.withLock {
                    if (activeIncomingId == message.pingId) {
                        finishIncoming(message.pingId)
                    }
                }
            }
            Log.i(TAG, "incoming ping ${message.pingId} from $fromName")
        }
    }

    private suspend fun onRemoteAck(message: ProtocolMessage.PingAck) {
        mutex.withLock {
            if (activeOutgoingId != message.pingId) return@withLock
            activeOutgoingId = null
            cancelOutgoingTimeout()
            _state.value = PingUiState.OutgoingResult(
                peerName = lastPeerName,
                outcome = message.response.toOutcome(),
            )
            Log.i(TAG, "ping ${message.pingId} -> ${message.response}")
        }
    }

    private suspend fun onRemoteCancel(message: ProtocolMessage.PingCancel) {
        mutex.withLock {
            if (activeIncomingId == message.pingId) {
                finishIncoming(message.pingId)
            }
        }
    }

    private fun finishIncoming(pingId: String) {
        if (activeIncomingId == pingId) {
            activeIncomingId = null
        }
        cancelIncomingTimeout()
        onIncomingCleared(pingId)
        if (_state.value is PingUiState.IncomingAlert) {
            val alert = _state.value as PingUiState.IncomingAlert
            if (alert.pingId == pingId) {
                _state.value = PingUiState.Idle
            }
        }
    }

    private fun clearIncomingLocal(reasonCancelRemote: Boolean) {
        val id = activeIncomingId ?: return
        if (reasonCancelRemote) {
            send(ProtocolMessage.PingCancel(id))
        }
        finishIncoming(id)
    }

    private fun cancelOutgoingTimeout() {
        outgoingTimeoutJob?.cancel()
        outgoingTimeoutJob = null
    }

    private fun cancelIncomingTimeout() {
        incomingTimeoutJob?.cancel()
        incomingTimeoutJob = null
    }

    companion object {
        private const val TAG = "PingController"
        const val PING_TIMEOUT_MS = 60_000L
    }
}
