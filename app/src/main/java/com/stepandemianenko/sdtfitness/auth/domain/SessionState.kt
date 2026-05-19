package com.stepandemianenko.sdtfitness.auth.domain

data class AuthenticatedUser(
    val accountId: String,
    val remoteUserId: String,
    val email: String,
    val displayName: String?,
    val authProvider: String
)

sealed interface SessionState {
    data object Checking : SessionState
    data object SignedOut : SessionState
    data object Guest : SessionState
    data class Authenticated(val user: AuthenticatedUser) : SessionState
}
