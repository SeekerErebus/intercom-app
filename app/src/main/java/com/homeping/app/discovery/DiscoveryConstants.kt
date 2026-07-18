package com.homeping.app.discovery

object DiscoveryConstants {
    /** DNS-SD type; trailing dot is conventional for Android NsdManager. */
    const val SERVICE_TYPE = "_homeping._tcp."

    /** Fixed control-plane port (TCP listener arrives in PR5). */
    const val CONTROL_PORT = 7529

    const val ATTR_DEVICE_ID = "deviceId"
    const val ATTR_NAME = "name"
    const val ATTR_VER = "ver"

    const val PROTOCOL_VERSION = "1"

    const val MULTICAST_LOCK_TAG = "homeping-mdns"
}
