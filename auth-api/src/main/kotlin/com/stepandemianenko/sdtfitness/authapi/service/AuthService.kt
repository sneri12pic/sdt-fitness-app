package com.stepandemianenko.sdtfitness.authapi.service

import com.stepandemianenko.sdtfitness.authapi.data.AuthRepository
import com.stepandemianenko.sdtfitness.authapi.domain.AuthResponse
import com.stepandemianenko.sdtfitness.authapi.domain.AuthUser
import com.stepandemianenko.sdtfitness.authapi.domain.DuplicateAccountException
import com.stepandemianenko.sdtfitness.authapi.domain.InvalidCredentialsException
import com.stepandemianenko.sdtfitness.authapi.security.AccessTokenService
import com.stepandemianenko.sdtfitness.authapi.security.PasswordHasher
import com.stepandemianenko.sdtfitness.authapi.security.RefreshTokenService
import com.stepandemianenko.sdtfitness.authapi.validation.AuthValidation
import java.time.Clock

class AuthService(
    private val repository: AuthRepository,
    private val passwordHasher: PasswordHasher,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val clock: Clock = Clock.systemUTC()
) {
    fun register(email: String, password: String): AuthResponse {
        val normalizedEmail = AuthValidation.normalizeEmail(email)
        val validPassword = AuthValidation.requirePassword(password)
        if (repository.findUserByEmail(normalizedEmail) != null) {
            throw DuplicateAccountException()
        }

        val now = clock.millis()
        val user = repository.createPasswordUser(
            email = normalizedEmail,
            passwordHash = passwordHasher.hash(validPassword),
            nowMillis = now
        )
        repository.updateLastLogin(user.id, now)
        return issueAuthResponse(user, now)
    }

    fun login(email: String, password: String): AuthResponse {
        val normalizedEmail = AuthValidation.normalizeEmail(email)
        val validPassword = AuthValidation.requirePassword(password)
        val user = repository.findUserByEmail(normalizedEmail)

        if (user == null || !passwordHasher.verify(validPassword, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val now = clock.millis()
        repository.updateLastLogin(user.id, now)
        return issueAuthResponse(user, now)
    }

    fun refresh(refreshToken: String): AuthResponse {
        val rawToken = AuthValidation.requireToken(refreshToken)
        val oldHash = refreshTokenService.hash(rawToken)
        val now = clock.millis()
        val newRefreshToken = refreshTokenService.issue(now)
        val user = repository.rotateRefreshToken(
            oldTokenHash = oldHash,
            newTokenHash = newRefreshToken.tokenHash,
            newExpiresAtMillis = newRefreshToken.expiresAtMillis,
            nowMillis = now
        ) ?: throw InvalidCredentialsException()

        val accessToken = accessTokenService.issue(user, now)
        return user.toAuthResponse(
            accessToken = accessToken.token,
            refreshToken = newRefreshToken.rawToken,
            accessTokenExpiresAtMillis = accessToken.expiresAtMillis
        )
    }

    fun signInWithCredential(credentialToken: String): AuthResponse {
        AuthValidation.requireToken(credentialToken)
        throw InvalidCredentialsException()
    }

    private fun issueAuthResponse(user: AuthUser, now: Long): AuthResponse {
        val accessToken = accessTokenService.issue(user, now)
        val refreshToken = refreshTokenService.issue(now)
        repository.createRefreshToken(
            userId = user.id,
            tokenHash = refreshToken.tokenHash,
            expiresAtMillis = refreshToken.expiresAtMillis,
            nowMillis = now
        )
        return user.toAuthResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.rawToken,
            accessTokenExpiresAtMillis = accessToken.expiresAtMillis
        )
    }

    private fun AuthUser.toAuthResponse(
        accessToken: String,
        refreshToken: String,
        accessTokenExpiresAtMillis: Long
    ): AuthResponse {
        return AuthResponse(
            remoteUserId = remoteUserId,
            email = email,
            displayName = displayName,
            authProvider = authProvider,
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresAtMillis = accessTokenExpiresAtMillis
        )
    }
}
