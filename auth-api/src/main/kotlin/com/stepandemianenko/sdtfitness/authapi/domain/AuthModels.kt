package com.stepandemianenko.sdtfitness.authapi.domain

import kotlinx.serialization.Serializable

const val PASSWORD_AUTH_PROVIDER = "password"

@Serializable
data class EmailPasswordRequest(
    val email: String = "",
    val password: String = ""
)

@Serializable
data class CredentialRequest(
    val credentialToken: String = ""
)

@Serializable
data class RefreshRequest(
    val refreshToken: String = ""
)

@Serializable
data class AuthResponse(
    val remoteUserId: String,
    val email: String,
    val displayName: String?,
    val authProvider: String,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtMillis: Long
)

@Serializable
data class ErrorResponse(
    val error: String
)

data class AuthUser(
    val id: Long,
    val remoteUserId: String,
    val email: String,
    val displayName: String?,
    val authProvider: String,
    val passwordHash: String
)
