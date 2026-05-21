package com.stepandemianenko.sdtfitness.authapi.validation

import com.stepandemianenko.sdtfitness.authapi.domain.ValidationException

object AuthValidation {
    private val emailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

    fun normalizeEmail(email: String): String {
        val normalized = email.trim().lowercase()
        if (normalized.length > 320 || !emailRegex.matches(normalized)) {
            throw ValidationException()
        }
        return normalized
    }

    fun requirePassword(password: String): String {
        if (password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
            throw ValidationException()
        }
        return password
    }

    fun requireToken(token: String): String {
        val trimmed = token.trim()
        if (trimmed.isBlank() || trimmed.length > MAX_TOKEN_LENGTH) {
            throw ValidationException()
        }
        return trimmed
    }

    private const val MIN_PASSWORD_LENGTH = 8
    private const val MAX_PASSWORD_LENGTH = 1024
    private const val MAX_TOKEN_LENGTH = 4096
}
