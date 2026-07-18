package com.homeping.app.ping

import com.homeping.app.net.PingResponse
import com.homeping.app.net.ProtocolMessage
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PingControllerTest {

    @Test
    fun sendPing_movesToRinging_andAckComing() = runTest {
        val sent = CopyOnWriteArrayList<ProtocolMessage>()
        val controller = PingController(
            scope = this,
            selfDeviceId = "self",
            selfDisplayName = "Upstairs",
            send = { msg -> sent.add(msg); true },
            onIncomingAlert = { _, _ -> },
            onIncomingCleared = {},
        )

        assertTrue(controller.sendPing("Downstairs"))
        assertTrue(controller.state.value is PingUiState.OutgoingRinging)
        val pingId = (controller.state.value as PingUiState.OutgoingRinging).pingId

        controller.handleRemote(ProtocolMessage.PingAck(pingId, PingResponse.Coming))
        // runCurrent only — avoid advanceUntilIdle which fires the 60s timeout.
        runCurrent()

        val result = controller.state.value as PingUiState.OutgoingResult
        assertEquals(OutgoingOutcome.Coming, result.outcome)
        assertTrue(sent.first() is ProtocolMessage.Ping)
    }

    @Test
    fun incomingPing_sendsDelivered_andAck() = runTest {
        val sent = CopyOnWriteArrayList<ProtocolMessage>()
        var alerted = false
        val controller = PingController(
            scope = this,
            selfDeviceId = "self",
            selfDisplayName = "Upstairs",
            send = { msg -> sent.add(msg); true },
            onIncomingAlert = { _, _ -> alerted = true },
            onIncomingCleared = {},
        )

        controller.handleRemote(
            ProtocolMessage.Ping(
                pingId = "p1",
                fromDeviceId = "other",
                fromName = "Mom",
                timestampMs = 1L,
            ),
        )
        runCurrent()

        assertTrue(alerted)
        assertTrue(controller.state.value is PingUiState.IncomingAlert)
        assertTrue(sent.any { it is ProtocolMessage.PingDelivered })

        controller.respondIncoming("p1", PingResponse.Coming)
        runCurrent()

        assertTrue(sent.any { it is ProtocolMessage.PingAck && it.response == PingResponse.Coming })
        assertEquals(PingUiState.Idle, controller.state.value)
    }
}
