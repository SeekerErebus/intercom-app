package com.homeping.app.data

/**
 * On-device identity and household setup state.
 * Peer pairing fields are reserved for discovery/auth PRs.
 */
data class UserPreferences(
    val deviceId: String,
    val displayName: String,
    val homePin: String,
    val setupComplete: Boolean,
    val pairedPeerId: String = "",
    val pairedPeerName: String = "",
    val alertSoundId: String = "default",
) {
    val hasIdentity: Boolean
        get() = deviceId.isNotBlank() && displayName.isNotBlank() && homePin.isNotBlank()
}
