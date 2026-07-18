package com.homeping.app.net

import org.json.JSONObject

object Protocol {
    const val VERSION = 1
    const val MAX_FRAME_BYTES = 64 * 1024
}

enum class PingResponse(val wire: String) {
    Coming("coming"),
    Dismissed("dismissed"),
    ;

    companion object {
        fun fromWire(value: String): PingResponse? =
            entries.firstOrNull { it.wire.equals(value, ignoreCase = true) }
    }
}

sealed class ProtocolMessage {
    abstract val type: String

    data class Hello(
        val deviceId: String,
        val displayName: String,
        val protocolVersion: Int,
        val nonce: String,
    ) : ProtocolMessage() {
        override val type: String = TYPE_HELLO
    }

    data class Auth(
        val deviceId: String,
        val proof: String,
    ) : ProtocolMessage() {
        override val type: String = TYPE_AUTH
    }

    data class AuthOk(
        val deviceId: String,
    ) : ProtocolMessage() {
        override val type: String = TYPE_AUTH_OK
    }

    data class AuthFail(
        val reason: String,
    ) : ProtocolMessage() {
        override val type: String = TYPE_AUTH_FAIL
    }

    data class Presence(
        val deviceId: String,
    ) : ProtocolMessage() {
        override val type: String = TYPE_PRESENCE
    }

    data class Ping(
        val pingId: String,
        val fromDeviceId: String,
        val fromName: String,
        val timestampMs: Long,
    ) : ProtocolMessage() {
        override val type: String = TYPE_PING
    }

    data class PingDelivered(
        val pingId: String,
    ) : ProtocolMessage() {
        override val type: String = TYPE_PING_DELIVERED
    }

    data class PingAck(
        val pingId: String,
        val response: PingResponse,
    ) : ProtocolMessage() {
        override val type: String = TYPE_PING_ACK
    }

    data class PingCancel(
        val pingId: String,
    ) : ProtocolMessage() {
        override val type: String = TYPE_PING_CANCEL
    }

    /** Opaque / future messages kept for forward compatibility. */
    data class Unknown(
        val rawType: String,
        val json: JSONObject,
    ) : ProtocolMessage() {
        override val type: String = rawType
    }

    fun toJson(): JSONObject {
        return when (this) {
            is Hello -> JSONObject()
                .put("type", type)
                .put("deviceId", deviceId)
                .put("displayName", displayName)
                .put("protocolVersion", protocolVersion)
                .put("nonce", nonce)
            is Auth -> JSONObject()
                .put("type", type)
                .put("deviceId", deviceId)
                .put("proof", proof)
            is AuthOk -> JSONObject()
                .put("type", type)
                .put("deviceId", deviceId)
            is AuthFail -> JSONObject()
                .put("type", type)
                .put("reason", reason)
            is Presence -> JSONObject()
                .put("type", type)
                .put("deviceId", deviceId)
            is Ping -> JSONObject()
                .put("type", type)
                .put("pingId", pingId)
                .put("fromDeviceId", fromDeviceId)
                .put("fromName", fromName)
                .put("timestamp", timestampMs)
            is PingDelivered -> JSONObject()
                .put("type", type)
                .put("pingId", pingId)
            is PingAck -> JSONObject()
                .put("type", type)
                .put("pingId", pingId)
                .put("response", response.wire)
            is PingCancel -> JSONObject()
                .put("type", type)
                .put("pingId", pingId)
            is Unknown -> json
        }
    }

    companion object {
        const val TYPE_HELLO = "hello"
        const val TYPE_AUTH = "auth"
        const val TYPE_AUTH_OK = "auth_ok"
        const val TYPE_AUTH_FAIL = "auth_fail"
        const val TYPE_PRESENCE = "presence"
        const val TYPE_PING = "ping"
        const val TYPE_PING_DELIVERED = "ping_delivered"
        const val TYPE_PING_ACK = "ping_ack"
        const val TYPE_PING_CANCEL = "ping_cancel"

        fun parse(json: JSONObject): ProtocolMessage {
            return when (val type = json.optString("type")) {
                TYPE_HELLO -> Hello(
                    deviceId = json.getString("deviceId"),
                    displayName = json.optString("displayName"),
                    protocolVersion = json.optInt("protocolVersion", Protocol.VERSION),
                    nonce = json.getString("nonce"),
                )
                TYPE_AUTH -> Auth(
                    deviceId = json.getString("deviceId"),
                    proof = json.getString("proof"),
                )
                TYPE_AUTH_OK -> AuthOk(deviceId = json.getString("deviceId"))
                TYPE_AUTH_FAIL -> AuthFail(reason = json.optString("reason", "rejected"))
                TYPE_PRESENCE -> Presence(deviceId = json.optString("deviceId"))
                TYPE_PING -> Ping(
                    pingId = json.getString("pingId"),
                    fromDeviceId = json.optString("fromDeviceId"),
                    fromName = json.optString("fromName"),
                    timestampMs = json.optLong("timestamp", System.currentTimeMillis()),
                )
                TYPE_PING_DELIVERED -> PingDelivered(pingId = json.getString("pingId"))
                TYPE_PING_ACK -> {
                    val response = PingResponse.fromWire(json.optString("response"))
                        ?: PingResponse.Dismissed
                    PingAck(pingId = json.getString("pingId"), response = response)
                }
                TYPE_PING_CANCEL -> PingCancel(pingId = json.getString("pingId"))
                else -> Unknown(rawType = type, json = json)
            }
        }

        fun parse(bytes: ByteArray): ProtocolMessage {
            val text = bytes.toString(Charsets.UTF_8)
            return parse(JSONObject(text))
        }
    }
}
