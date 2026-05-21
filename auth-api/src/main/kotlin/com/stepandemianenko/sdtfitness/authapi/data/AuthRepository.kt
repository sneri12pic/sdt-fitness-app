package com.stepandemianenko.sdtfitness.authapi.data

import com.stepandemianenko.sdtfitness.authapi.domain.AuthUser

interface AuthRepository {
    fun findUserByEmail(email: String): AuthUser?
    fun createPasswordUser(email: String, passwordHash: String, nowMillis: Long): AuthUser
    fun updateLastLogin(userId: Long, nowMillis: Long)
    fun createRefreshToken(userId: Long, tokenHash: String, expiresAtMillis: Long, nowMillis: Long)
    fun rotateRefreshToken(oldTokenHash: String, newTokenHash: String, newExpiresAtMillis: Long, nowMillis: Long): AuthUser?
    fun revokeAllRefreshTokens(userId: Long, nowMillis: Long)
}
