package com.kidloop.app.util

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure random 16-byte salt as a hex string.
     */
    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.toHexString()
    }

    /**
     * Computes a salted SHA-256 hash of the given PIN.
     * Returns a pair of (salt, hashHex).
     */
    fun hashPin(pin: String, salt: String = generateSalt()): Pair<String, String> {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt$pin".toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(combined)
        return Pair(salt, hashBytes.toHexString())
    }

    /**
     * Verifies an entered PIN against the stored salt and hash using constant-time comparison
     * to prevent side-channel timing attacks.
     */
    fun verifyPin(inputPin: String, salt: String, expectedHash: String): Boolean {
        if (salt.isBlank() || expectedHash.isBlank() || inputPin.isBlank()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt$inputPin".toByteArray(Charsets.UTF_8)
        val computedHashBytes = digest.digest(combined)
        val expectedHashBytes = expectedHash.hexToByteArrayOrNull() ?: return false
        return MessageDigest.isEqual(computedHashBytes, expectedHashBytes)
    }

    private fun ByteArray.toHexString(): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(size * 2)
        for (b in this) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    private fun String.hexToByteArrayOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        val result = ByteArray(length / 2)
        try {
            for (i in indices step 2) {
                val byteVal = substring(i, i + 2).toInt(16)
                result[i / 2] = byteVal.toByte()
            }
            return result
        } catch (_: Exception) {
            return null
        }
    }
}
