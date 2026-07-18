package com.homeping.app.data

object SetupValidation {
    const val NAME_MIN = 1
    const val NAME_MAX = 32
    const val PIN_MIN = 4
    const val PIN_MAX = 6

    fun normalizeName(raw: String): String = raw.trim()

    fun isValidDisplayName(name: String): Boolean {
        val n = normalizeName(name)
        return n.length in NAME_MIN..NAME_MAX
    }

    fun isValidPin(pin: String): Boolean {
        return pin.length in PIN_MIN..PIN_MAX && pin.all { it.isDigit() }
    }

    fun pinsMatch(pin: String, confirm: String): Boolean = pin == confirm
}
