package com.homeping.app.ping

import com.homeping.app.net.PingResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide ping API for UI and notification actions.
 */
object PingHub {
    private val _state = MutableStateFlow<PingUiState>(PingUiState.Idle)
    val state: StateFlow<PingUiState> = _state.asStateFlow()

    @Volatile
    private var controller: PingController? = null

    @Volatile
    private var canSend: () -> Boolean = { false }

    fun attach(controller: PingController, canSend: () -> Boolean) {
        this.controller = controller
        this.canSend = canSend
        // Mirror controller state
        // Collector is started by service; also publish immediate value.
        _state.value = controller.state.value
    }

    fun publish(state: PingUiState) {
        _state.value = state
    }

    fun detach() {
        controller = null
        canSend = { false }
        _state.value = PingUiState.Idle
    }

    fun isReadyToPing(): Boolean = canSend()

    fun sendPing(peerName: String): Boolean {
        val c = controller ?: return false
        if (!canSend()) return false
        return c.sendPing(peerName)
    }

    fun respond(pingId: String, response: PingResponse) {
        controller?.respondIncoming(pingId, response)
    }

    fun cancelOutgoing() {
        controller?.cancelOutgoing()
    }
}
