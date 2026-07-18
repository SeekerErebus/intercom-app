package com.homeping.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupValidationTest {

    @Test
    fun displayName_acceptsReasonableLabels() {
        assertTrue(SetupValidation.isValidDisplayName("Upstairs"))
        assertTrue(SetupValidation.isValidDisplayName("Mom's phone"))
        assertFalse(SetupValidation.isValidDisplayName("   "))
        assertFalse(SetupValidation.isValidDisplayName(""))
        assertFalse(SetupValidation.isValidDisplayName("x".repeat(33)))
    }

    @Test
    fun pin_requiresFourToSixDigits() {
        assertTrue(SetupValidation.isValidPin("1234"))
        assertTrue(SetupValidation.isValidPin("123456"))
        assertFalse(SetupValidation.isValidPin("123"))
        assertFalse(SetupValidation.isValidPin("1234567"))
        assertFalse(SetupValidation.isValidPin("12ab"))
        assertFalse(SetupValidation.isValidPin("12 34"))
    }

    @Test
    fun pinsMatch_isExact() {
        assertTrue(SetupValidation.pinsMatch("4242", "4242"))
        assertFalse(SetupValidation.pinsMatch("4242", "4243"))
    }
}
