package com.homeping.app.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceNameUtilTest {

    @Test
    fun build_includesSanitizedNameAndIdSuffix() {
        val name = ServiceNameUtil.build("Mom's phone", "abcdef12-9999")
        assertTrue(name.startsWith("Moms-phone") || name.startsWith("Mom-s-phone") || name.contains("Mom"))
        assertTrue(name.contains("abcdef12") || name.endsWith("abcdef12") || name.contains("-abcdef"))
    }

    @Test
    fun build_handlesBlankName() {
        val name = ServiceNameUtil.build("   ", "deadbeef-1111")
        assertTrue(name.startsWith("HomePing-"))
        assertTrue(name.contains("deadbeef"))
    }

    @Test
    fun build_stripsOddCharacters() {
        val name = ServiceNameUtil.build("Upstairs!!!", "12345678-xxxx")
        assertFalse(name.contains("!"))
        assertEquals("Upstairs-12345678", name)
    }
}
