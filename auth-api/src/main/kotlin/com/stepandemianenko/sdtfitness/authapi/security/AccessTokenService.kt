package com.stepandemianenko.sdtfitness.authapi.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.stepandemianenko.sdtfitness.authapi.config.AuthConfig
import com.stepandemianenko.sdtfitness.authapi.domain.AuthUser
import java.util.Date

data class IssuedAccessToken(
    val token: String,
    val expiresAtMillis: Long
)

class AccessTokenService(
    private val config: AuthConfig
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun issue(user: AuthUser, nowMillis: Long): IssuedAccessToken {
        val expiresAtMillis = nowMillis + config.accessTokenTtlMillis
        val token = JWT.create()
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withSubject(user.remoteUserId)
            .withClaim("email", user.email)
            .withClaim("authProvider", user.authProvider)
            .withIssuedAt(Date(nowMillis))
            .withExpiresAt(Date(expiresAtMillis))
            .sign(algorithm)

        return IssuedAccessToken(
            token = token,
            expiresAtMillis = expiresAtMillis
        )
    }
}
