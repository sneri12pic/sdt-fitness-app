package com.stepandemianenko.sdtfitness.auth.viewmodel

sealed interface AuthFormValidationResult {
    data object Valid : AuthFormValidationResult
    data class Invalid(val message: String) : AuthFormValidationResult
}

object AuthFormValidator {
    const val MIN_PASSWORD_LENGTH = 8

    fun validateLogin(email: String, password: String): AuthFormValidationResult {
        return if (email.isBlank() || password.isBlank()) {
            AuthFormValidationResult.Invalid("Enter your email and password.")
        } else {
            AuthFormValidationResult.Valid
        }
    }

    fun validateRegistration(
        email: String,
        password: String,
        confirmPassword: String
    ): AuthFormValidationResult {
        return when {
            email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                AuthFormValidationResult.Invalid("Enter your email, password, and confirmation.")
            }

            password != confirmPassword -> {
                AuthFormValidationResult.Invalid("Passwords do not match.")
            }

            password.length < MIN_PASSWORD_LENGTH -> {
                AuthFormValidationResult.Invalid("Password must be at least $MIN_PASSWORD_LENGTH characters.")
            }

            else -> AuthFormValidationResult.Valid
        }
    }
}
