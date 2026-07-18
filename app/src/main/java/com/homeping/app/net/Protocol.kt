package com.homeping.app.net

import org.json.JSONObject

object Protocol {
    const val VERSION = 1
    const val MAX_FRAME_BYTES = 64 * 1024
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
                else -> Unknown(rawType = type, json = json)
            }
        }

        fun parse(bytes: ByteArray): ProtocolMessage {
            val text = bytes.toString(Charsets.UTF_8)
            return parse(JSONObject(text))
        }
    }
}
