package com.homeping.app.net

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Shared-PIN proof for LAN peers.
 *
 * proof = hex(SHA-256( UTF-8( PIN || nonce_peer || nonce_self || deviceId_self ) ))
 *
 * Household deterrent only — not strong against a determined LAN attacker.
 */
object PinAuth {
    private val random = SecureRandom()

    fun newNonce(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.toHex()
    }

    /**
     * @param pin shared home PIN
     * @param noncePeer the other side's hello nonce
     * @param nonceSelf this side's hello nonce
     * @param deviceIdSelf this side's device id (the prover)
     */
    fun computeProof(
        pin: String,
        noncePeer: String,
        nonceSelf: String,
        deviceIdSelf: String,
    ): String {
        val material = pin + noncePeer + nonceSelf + deviceIdSelf
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(material.toByteArray(Charsets.UTF_8))
        return digest.toHex()
    }

    fun verifyProof(
        pin: String,
        nonceLocal: String,
        nonceRemote: String,
        deviceIdRemote: String,
        proof: String,
    ): Boolean {
        val expected = computeProof(
            pin = pin,
            noncePeer = nonceLocal,
            nonceSelf = nonceRemote,
            deviceIdSelf = deviceIdRemote,
        )
        return expected.equals(proof, ignoreCase = true)
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
