package com.stepandemianenko.sdtfitness.auth.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFormValidatorTest {

    @Test
    fun validateLogin_blankFields_returnsInvalidMessage() {
        val result = AuthFormValidator.validateLogin(email = "", password = "")

        assertEquals(
            AuthFormValidationResult.Invalid("Enter your email and password."),
            result
        )
    }

    @Test
    fun validateRegistration_blankFields_returnsInvalidMessage() {
        val result = AuthFormValidator.validateRegistration(
            email = "",
            password = "",
            confirmPassword = ""
        )

        assertEquals(
            AuthFormValidationResult.Invalid("Enter your email, password, and confirmation."),
            result
        )
    }

    @Test
    fun validateRegistration_mismatchedPasswords_returnsInvalidMessage() {
        val result = AuthFormValidator.validateRegistration(
            email = "user@example.com",
            password = "password123",
            confirmPassword = "password456"
        )

        assertEquals(
            AuthFormValidationResult.Invalid("Passwords do not match."),
            result
        )
    }

    @Test
    fun validateRegistration_shortPassword_returnsInvalidMessage() {
        val result = AuthFormValidator.validateRegistration(
            email = "user@example.com",
            password = "short",
            confirmPassword = "short"
        )

        assertEquals(
            AuthFormValidationResult.Invalid("Password must be at least 8 characters."),
            result
        )
    }

    @Test
    fun validateRegistration_validFields_returnsValid() {
        val result = AuthFormValidator.validateRegistration(
            email = "user@example.com",
            password = "password123",
            confirmPassword = "password123"
        )

        assertTrue(result is AuthFormValidationResult.Valid)
    }
}
