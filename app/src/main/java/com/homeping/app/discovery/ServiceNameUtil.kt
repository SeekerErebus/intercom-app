package com.homeping.app.discovery

object ServiceNameUtil {
    /**
     * NSD service names should be relatively short and avoid characters that
     * break DNS labels. Uniqueness comes from a device-id suffix.
     */
    fun build(displayName: String, deviceId: String): String {
        val base = displayName
            .trim()
            .ifBlank { "HomePing" }
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .replace(Regex("\\s+"), "-")
            .take(24)
            .trim('-')
            .ifBlank { "HomePing" }
        val suffix = deviceId.filter { it.isLetterOrDigit() }.take(8).ifBlank { "device" }
        return "$base-$suffix"
    }
}
