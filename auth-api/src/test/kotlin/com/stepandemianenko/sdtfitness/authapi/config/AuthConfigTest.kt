package com.stepandemianenko.sdtfitness.authapi.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AuthConfigTest {

    @Test
    fun `development keeps local defaults`() {
        val config = AuthConfig.fromEnvironment(emptyMap())

        assertEquals("0.0.0.0", config.host)
        assertEquals(8080, config.port)
        assertFalse(config.isProduction)
        assertFalse(config.trustProxyHeaders)
    }

    @Test
    fun `production requires PostgreSQL configuration`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            AuthConfig.fromEnvironment(
                mapOf(
                    "AUTH_ENVIRONMENT" to "production",
                    "AUTH_JWT_SECRET" to "jwt-secret-that-is-at-least-32-characters",
                    "AUTH_REFRESH_TOKEN_PEPPER" to "refresh-pepper-at-least-32-characters"
                )
            )
        }

        assertEquals("AUTH_DATABASE_URL is required in production.", exception.message)
    }

    @Test
    fun `production accepts complete independent secrets and PostgreSQL settings`() {
        val config = AuthConfig.fromEnvironment(
            mapOf(
                "AUTH_ENVIRONMENT" to "production",
                "AUTH_DATABASE_URL" to "jdbc:postgresql://postgres:5432/sdt_fitness_auth",
                "AUTH_DATABASE_USER" to "sdt_auth",
                "AUTH_DATABASE_PASSWORD" to "database-password",
                "AUTH_JWT_SECRET" to "jwt-secret-that-is-at-least-32-characters",
                "AUTH_REFRESH_TOKEN_PEPPER" to "refresh-pepper-at-least-32-characters",
                "AUTH_TRUST_PROXY_HEADERS" to "true"
            )
        )

        assertEquals("jdbc:postgresql://postgres:5432/sdt_fitness_auth", config.databaseUrl)
        assertEquals(true, config.trustProxyHeaders)
    }

    @Test
    fun `production rejects reused token secrets`() {
        val sharedSecret = "shared-secret-that-is-at-least-32-characters"

        assertFailsWith<IllegalArgumentException> {
            AuthConfig.fromEnvironment(
                mapOf(
                    "AUTH_ENVIRONMENT" to "production",
                    "AUTH_DATABASE_URL" to "jdbc:postgresql://postgres:5432/sdt_fitness_auth",
                    "AUTH_DATABASE_USER" to "sdt_auth",
                    "AUTH_DATABASE_PASSWORD" to "database-password",
                    "AUTH_JWT_SECRET" to sharedSecret,
                    "AUTH_REFRESH_TOKEN_PEPPER" to sharedSecret
                )
            )
        }
    }
}
