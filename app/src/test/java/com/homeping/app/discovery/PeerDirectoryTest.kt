package com.homeping.app.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PeerDirectoryTest {

    @Before
    fun setUp() {
        PeerDirectory.resetForTests()
    }

    @Test
    fun upsert_andSelectPrimary_prefersPairedId() {
        PeerDirectory.upsert(
            "svc-a",
            DiscoveredPeer("id-a", "Upstairs", "192.168.1.2", 7529, "svc-a"),
        )
        PeerDirectory.upsert(
            "svc-b",
            DiscoveredPeer("id-b", "Downstairs", "192.168.1.3", 7529, "svc-b"),
        )
        assertEquals("id-b", PeerDirectory.selectPrimary("id-b")?.deviceId)
        assertEquals("Downstairs", PeerDirectory.selectPrimary(null)?.displayName)
    }

    @Test
    fun remove_clearsPeer() {
        PeerDirectory.upsert(
            "svc-a",
            DiscoveredPeer("id-a", "Upstairs", "192.168.1.2", 7529, "svc-a"),
        )
        PeerDirectory.removeByServiceName("svc-a")
        assertNull(PeerDirectory.selectPrimary(null))
        assertEquals(0, PeerDirectory.peers.value.size)
    }

    @Test
    fun upsert_replacesSameDeviceIdUnderNewServiceName() {
        PeerDirectory.upsert(
            "old",
            DiscoveredPeer("id-a", "Old", "192.168.1.2", 7529, "old"),
        )
        PeerDirectory.upsert(
            "new",
            DiscoveredPeer("id-a", "New", "192.168.1.2", 7529, "new"),
        )
        assertEquals(1, PeerDirectory.peers.value.size)
        assertEquals("New", PeerDirectory.peers.value.first().displayName)
    }
}
