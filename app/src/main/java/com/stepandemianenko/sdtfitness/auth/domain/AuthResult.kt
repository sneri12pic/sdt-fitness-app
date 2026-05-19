package com.stepandemianenko.sdtfitness.auth.domain

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val reason: AuthFailureReason) : AuthResult
}

enum class AuthFailureReason {
    InvalidInput,
    AccountAlreadyExists,
    ServiceUnavailable,
    InvalidCredentials,
    Network,
    Unknown
}
