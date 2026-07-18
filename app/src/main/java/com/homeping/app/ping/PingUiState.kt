package com.homeping.app.ping

import com.homeping.app.net.PingResponse

/**
 * UI-facing ping state for the primary peer session.
 */
sealed class PingUiState {
    data object Idle : PingUiState()

    data class OutgoingRinging(
        val pingId: String,
        val peerName: String,
    ) : PingUiState()

    data class OutgoingResult(
        val peerName: String,
        val outcome: OutgoingOutcome,
    ) : PingUiState()

    data class IncomingAlert(
        val pingId: String,
        val fromName: String,
    ) : PingUiState()
}

enum class OutgoingOutcome {
    Coming,
    Dismissed,
    Timeout,
    Failed,
    Cancelled,
}

fun PingResponse.toOutcome(): OutgoingOutcome = when (this) {
    PingResponse.Coming -> OutgoingOutcome.Coming
    PingResponse.Dismissed -> OutgoingOutcome.Dismissed
}
