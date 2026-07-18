package com.homeping.app.discovery

/**
 * A HomePing instance seen on the LAN via mDNS/NSD.
 */
data class DiscoveredPeer(
    val deviceId: String,
    val displayName: String,
    val host: String,
    val port: Int,
    val serviceName: String,
    val protocolVersion: Int = 1,
    val lastSeenEpochMs: Long = System.currentTimeMillis(),
) {
    val shortId: String
        get() = deviceId.take(8).ifBlank { "unknown" }
}
