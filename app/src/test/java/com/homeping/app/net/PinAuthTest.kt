package com.homeping.app.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinAuthTest {

    @Test
    fun proof_roundTrip_withMatchingPin() {
        val pin = "4242"
        val nonceA = "aaaabbbbccccdddd"
        val nonceB = "1111222233334444"
        val deviceB = "device-b-uuid"

        val proof = PinAuth.computeProof(
            pin = pin,
            noncePeer = nonceA,
            nonceSelf = nonceB,
            deviceIdSelf = deviceB,
        )
        assertTrue(
            PinAuth.verifyProof(
                pin = pin,
                nonceLocal = nonceA,
                nonceRemote = nonceB,
                deviceIdRemote = deviceB,
                proof = proof,
            ),
        )
    }

    @Test
    fun proof_failsWithWrongPin() {
        val proof = PinAuth.computeProof("4242", "n1", "n2", "dev")
        assertFalse(PinAuth.verifyProof("9999", "n1", "n2", "dev", proof))
    }

    @Test
    fun nonces_areUnique() {
        val a = PinAuth.newNonce()
        val b = PinAuth.newNonce()
        assertEquals(32, a.length)
        assertNotEquals(a, b)
    }
}
