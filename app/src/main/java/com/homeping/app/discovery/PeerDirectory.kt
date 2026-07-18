package com.homeping.app.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory view of peers currently advertised on the LAN.
 * Updated by [NsdPeerDiscovery]; observed by UI without binding the service.
 */
object PeerDirectory {
    private val byServiceName = linkedMapOf<String, DiscoveredPeer>()
    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val peers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    @Synchronized
    fun upsert(serviceName: String, peer: DiscoveredPeer) {
        // Drop stale service names for the same device id (re-register after rename).
        val staleKeys = byServiceName.filter { (key, value) ->
            key != serviceName &&
                value.deviceId.isNotBlank() &&
                value.deviceId == peer.deviceId
        }.keys.toList()
        staleKeys.forEach { byServiceName.remove(it) }
        byServiceName[serviceName] = peer
        publishLocked()
    }

    @Synchronized
    fun removeByServiceName(serviceName: String) {
        if (byServiceName.remove(serviceName) != null) {
            publishLocked()
        }
    }

    @Synchronized
    fun clear() {
        if (byServiceName.isNotEmpty()) {
            byServiceName.clear()
            publishLocked()
        }
    }

    /**
     * Prefer a previously paired device; otherwise the first discovered peer.
     */
    fun selectPrimary(pairedPeerId: String?): DiscoveredPeer? {
        val list = _peers.value
        if (list.isEmpty()) return null
        if (!pairedPeerId.isNullOrBlank()) {
            list.firstOrNull { it.deviceId == pairedPeerId }?.let { return it }
        }
        return list.firstOrNull()
    }

    private fun publishLocked() {
        _peers.value = byServiceName.values
            .sortedWith(compareBy({ it.displayName.lowercase() }, { it.deviceId }))
    }

    /** Test helper */
    internal fun resetForTests() {
        clear()
        _peers.update { emptyList() }
    }
}
