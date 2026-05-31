package com.skladpro.employees.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordHasher(
    private val iterations: Int = 120_000
) {
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(password, salt, iterations)
        return listOf(
            iterations.toString(),
            encoder.encodeToString(salt),
            encoder.encodeToString(hash)
        ).joinToString(SEPARATOR)
    }

    fun matches(password: String, encodedHash: String): Boolean {
        val parts = encodedHash.split(SEPARATOR)
        if (parts.size != 3) return false

        val parsedIterations = parts[0].toIntOrNull() ?: return false
        val salt = runCatching { decoder.decode(parts[1]) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(parts[2]) }.getOrNull() ?: return false
        val actual = derive(password, salt, parsedIterations)
        return actual.contentEquals(expected)
    }

    private fun derive(password: String, salt: ByteArray, iterationCount: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterationCount, HASH_BITS)
        return SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    private companion object {
        const val SALT_BYTES = 16
        const val HASH_BITS = 256
        const val SEPARATOR = ":"
        val encoder: Base64.Encoder = Base64.getEncoder()
        val decoder: Base64.Decoder = Base64.getDecoder()
    }
}
