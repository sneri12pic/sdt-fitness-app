package com.stepandemianenko.sdtfitness.authapi

import com.stepandemianenko.sdtfitness.authapi.config.AuthConfig
import com.stepandemianenko.sdtfitness.authapi.domain.AuthResponse
import com.stepandemianenko.sdtfitness.authapi.domain.EmailPasswordRequest
import com.stepandemianenko.sdtfitness.authapi.domain.RefreshRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthApiTest {

    @Test
    fun `register creates an account and returns Android-compatible response`() = testApplication {
        application { authApiModule(testConfig()) }
        val client = jsonClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(EmailPasswordRequest(" USER@example.COM ", "password123"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertEquals("user@example.com", body.email)
        assertEquals("password", body.authProvider)
        assertTrue(body.remoteUserId.isNotBlank())
        assertTrue(body.accessToken.isNotBlank())
        assertTrue(body.refreshToken.isNotBlank())
        assertTrue(body.accessTokenExpiresAtMillis > 0)
    }

    @Test
    fun `duplicate registration returns conflict`() = testApplication {
        application { authApiModule(testConfig()) }
        val client = jsonClient()
        val request = EmailPasswordRequest("dupe@example.com", "password123")

        assertEquals(HttpStatusCode.OK, client.register(request).status)
        assertEquals(HttpStatusCode.Conflict, client.register(request).status)
    }

    @Test
    fun `login succeeds with registered credentials`() = testApplication {
        application { authApiModule(testConfig()) }
        val client = jsonClient()
        val request = EmailPasswordRequest("login@example.com", "password123")

        client.register(request)
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("login@example.com", response.body<AuthResponse>().email)
    }

    @Test
    fun `invalid login returns unauthorized`() = testApplication {
        application { authApiModule(testConfig()) }
        val client = jsonClient()
        client.register(EmailPasswordRequest("invalid@example.com", "password123"))

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(EmailPasswordRequest("invalid@example.com", "wrongpassword"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `refresh rotates refresh token and rejects reuse`() = testApplication {
        application { authApiModule(testConfig()) }
        val client = jsonClient()
        val registered = client.register(EmailPasswordRequest("refresh@example.com", "password123"))
            .body<AuthResponse>()

        val refreshedResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(registered.refreshToken))
        }

        assertEquals(HttpStatusCode.OK, refreshedResponse.status)
        val refreshed = refreshedResponse.body<AuthResponse>()
        assertNotEquals(registered.refreshToken, refreshed.refreshToken)

        val reusedResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(registered.refreshToken))
        }

        assertEquals(HttpStatusCode.Unauthorized, reusedResponse.status)
    }

    @Test
    fun `expired refresh token returns unauthorized`() = testApplication {
        application { authApiModule(testConfig(refreshTtlMillis = 1L)) }
        val client = jsonClient()
        val registered = client.register(EmailPasswordRequest("expired@example.com", "password123"))
            .body<AuthResponse>()
        delay(10)

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(registered.refreshToken))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun testConfig(refreshTtlMillis: Long = 30L * 24L * 60L * 60L * 1000L): AuthConfig {
        return AuthConfig(
            host = "127.0.0.1",
            port = 0,
            databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            databaseUser = "sa",
            databasePassword = "",
            jwtIssuer = "sdt-fitness-auth-api-test",
            jwtAudience = "sdt-fitness-android-test",
            jwtSecret = "test-jwt-secret-with-enough-length-123",
            refreshTokenPepper = "test-refresh-pepper-with-enough-length",
            accessTokenTtlMillis = 15 * 60 * 1000L,
            refreshTokenTtlMillis = refreshTtlMillis,
            bcryptCost = 4,
            rateLimitMaxRequests = 100,
            rateLimitWindowMillis = 60_000L,
            allowedOrigins = emptyList(),
            trustProxyHeaders = false,
            environment = "test"
        )
    }
}

private fun io.ktor.server.testing.ApplicationTestBuilder.jsonClient() = createClient {
    install(ContentNegotiation) {
        json()
    }
}

private suspend fun io.ktor.client.HttpClient.register(request: EmailPasswordRequest) = post("/auth/register") {
    contentType(ContentType.Application.Json)
    setBody(request)
}
