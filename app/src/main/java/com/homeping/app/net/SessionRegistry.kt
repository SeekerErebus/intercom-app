package com.homeping.app.net

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide view of the primary peer connection for UI observation.
 * [SessionManager] publishes here; Compose collects without binding.
 */
object SessionRegistry {
    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    fun publish(status: ConnectionStatus) {
        _status.value = status
    }

    fun reset() {
        _status.value = ConnectionStatus.Idle
    }
}
