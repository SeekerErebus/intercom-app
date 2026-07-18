package com.homeping.app.net

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameIoTest {

    @Test
    fun writeAndRead_helloRoundTrip() {
        val original = ProtocolMessage.Hello(
            deviceId = "id-1",
            displayName = "Upstairs",
            protocolVersion = 1,
            nonce = "abcd",
        )
        val baos = ByteArrayOutputStream()
        FrameIo.writeMessage(baos, original)
        val parsed = FrameIo.readMessage(ByteArrayInputStream(baos.toByteArray()))
        assertTrue(parsed is ProtocolMessage.Hello)
        val hello = parsed as ProtocolMessage.Hello
        assertEquals("id-1", hello.deviceId)
        assertEquals("Upstairs", hello.displayName)
        assertEquals("abcd", hello.nonce)
    }

    @Test
    fun lengthHeader_isBigEndian() {
        val header = FrameIo.lengthHeader(0x00000100)
        assertEquals(0, header[0].toInt())
        assertEquals(0, header[1].toInt())
        assertEquals(1, header[2].toInt())
        assertEquals(0, header[3].toInt())
    }
}
