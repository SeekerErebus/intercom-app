package com.homeping.app.net

/**
 * High-level link state for the primary peer (two-device v1).
 */
sealed class ConnectionStatus {
    data object Idle : ConnectionStatus()

    data object Listening : ConnectionStatus()

    data class Connecting(
        val peerName: String,
        val host: String,
    ) : ConnectionStatus()

    data class Handshaking(
        val peerName: String,
        val host: String,
    ) : ConnectionStatus()

    data class Authenticated(
        val peerId: String,
        val peerName: String,
        val host: String,
    ) : ConnectionStatus()

    data class Failed(
        val peerName: String?,
        val reason: String,
    ) : ConnectionStatus()

    val isAuthenticated: Boolean
        get() = this is Authenticated
}
