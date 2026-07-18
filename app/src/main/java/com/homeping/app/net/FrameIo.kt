package com.homeping.app.net

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Length-prefixed frames: 4-byte big-endian size + UTF-8 payload.
 */
object FrameIo {
    fun writeFrame(output: OutputStream, payload: ByteArray) {
        if (payload.size > Protocol.MAX_FRAME_BYTES) {
            throw IOException("frame too large: ${payload.size}")
        }
        val out = DataOutputStream(output)
        out.writeInt(payload.size)
        out.write(payload)
        out.flush()
    }

    fun writeMessage(output: OutputStream, message: ProtocolMessage) {
        val bytes = message.toJson().toString().toByteArray(Charsets.UTF_8)
        writeFrame(output, bytes)
    }

    fun readFrame(input: InputStream): ByteArray {
        val din = DataInputStream(input)
        val length = try {
            din.readInt()
        } catch (e: EOFException) {
            throw e
        }
        if (length < 0 || length > Protocol.MAX_FRAME_BYTES) {
            throw IOException("invalid frame length: $length")
        }
        val buf = ByteArray(length)
        din.readFully(buf)
        return buf
    }

    fun readMessage(input: InputStream): ProtocolMessage {
        return ProtocolMessage.parse(readFrame(input))
    }

    /** Test helper: encode length header only. */
    fun lengthHeader(size: Int): ByteArray {
        return ByteBuffer.allocate(4).putInt(size).array()
    }
}
