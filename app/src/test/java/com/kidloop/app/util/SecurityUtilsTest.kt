package com.kidloop.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `generate salt returns 32 char hex string`() {
        val salt = SecurityUtils.generateSalt()
        assertEquals(32, salt.length)
        assertTrue(salt.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `unique salts generated for consecutive calls`() {
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun `hashPin produces 64 character sha-256 hex string`() {
        val (salt, hash) = SecurityUtils.hashPin("1234")
        assertEquals(32, salt.length)
        assertEquals(64, hash.length)
    }

    @Test
    fun `verifyPin returns true for correct pin and salt`() {
        val (salt, hash) = SecurityUtils.hashPin("4567")
        val isVerified = SecurityUtils.verifyPin("4567", salt, hash)
        assertTrue(isVerified)
    }

    @Test
    fun `verifyPin returns false for wrong pin`() {
        val (salt, hash) = SecurityUtils.hashPin("4567")
        val isVerified = SecurityUtils.verifyPin("9999", salt, hash)
        assertFalse(isVerified)
    }

    @Test
    fun `verifyPin returns false for blank inputs`() {
        assertFalse(SecurityUtils.verifyPin("", "salt", "hash"))
        assertFalse(SecurityUtils.verifyPin("1234", "", "hash"))
        assertFalse(SecurityUtils.verifyPin("1234", "salt", ""))
    }
}
