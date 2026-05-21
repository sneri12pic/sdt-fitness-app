package com.stepandemianenko.sdtfitness.authapi.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class IssuedRefreshToken(
    val rawToken: String,
    val tokenHash: String,
    val expiresAtMillis: Long
)

class RefreshTokenService(
    private val pepper: String,
    private val ttlMillis: Long,
    private val random: SecureRandom = SecureRandom()
) {
    fun issue(nowMillis: Long): IssuedRefreshToken {
        val tokenBytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(tokenBytes)
        val rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)
        return IssuedRefreshToken(
            rawToken = rawToken,
            tokenHash = hash(rawToken),
            expiresAtMillis = nowMillis + ttlMillis
        )
    }

    fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$pepper:$rawToken".toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private companion object {
        const val TOKEN_BYTES = 64
    }
}
