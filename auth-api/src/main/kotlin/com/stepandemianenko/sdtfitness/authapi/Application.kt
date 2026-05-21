package com.stepandemianenko.sdtfitness.authapi

import com.stepandemianenko.sdtfitness.authapi.config.AuthConfig
import com.stepandemianenko.sdtfitness.authapi.data.DatabaseFactory
import com.stepandemianenko.sdtfitness.authapi.data.JdbcAuthRepository
import com.stepandemianenko.sdtfitness.authapi.domain.AuthApiException
import com.stepandemianenko.sdtfitness.authapi.domain.ErrorResponse
import com.stepandemianenko.sdtfitness.authapi.domain.HttpsRequiredException
import com.stepandemianenko.sdtfitness.authapi.rate.FixedWindowRateLimiter
import com.stepandemianenko.sdtfitness.authapi.routing.authRoutes
import com.stepandemianenko.sdtfitness.authapi.security.AccessTokenService
import com.stepandemianenko.sdtfitness.authapi.security.PasswordHasher
import com.stepandemianenko.sdtfitness.authapi.security.RefreshTokenService
import com.stepandemianenko.sdtfitness.authapi.service.AuthService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val config = AuthConfig.fromEnvironment()
    embeddedServer(
        factory = Netty,
        port = config.port,
        module = { authApiModule(config) }
    ).start(wait = true)
}

fun Application.authApiModule(config: AuthConfig = AuthConfig.fromEnvironment()) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = false
                explicitNulls = true
            }
        )
    }

    install(StatusPages) {
        exception<AuthApiException> { call, cause ->
            call.respond(cause.statusCode, ErrorResponse(cause.safeErrorCode))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_request"))
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("service_unavailable"))
        }
    }

    if (config.allowedOrigins.isNotEmpty()) {
        install(CORS) {
            config.allowedOrigins.forEach { allowHost(it.removePrefix("https://"), schemes = listOf("https")) }
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Options)
        }
    }

    if (config.isProduction) {
        intercept(ApplicationCallPipeline.Plugins) {
            val forwardedProto = call.request.header("X-Forwarded-Proto")
            val scheme = forwardedProto ?: call.request.origin.scheme
            if (!scheme.equals("https", ignoreCase = true)) {
                throw HttpsRequiredException()
            }
        }
    }

    val dataSource = DatabaseFactory.createDataSource(config)
    DatabaseFactory.migrate(dataSource)
    environment.monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        dataSource.close()
    }

    val repository = JdbcAuthRepository(dataSource)
    val authService = AuthService(
        repository = repository,
        passwordHasher = PasswordHasher(config.bcryptCost),
        accessTokenService = AccessTokenService(config),
        refreshTokenService = RefreshTokenService(
            pepper = config.refreshTokenPepper,
            ttlMillis = config.refreshTokenTtlMillis
        )
    )
    val rateLimiter = FixedWindowRateLimiter(
        maxRequests = config.rateLimitMaxRequests,
        windowMillis = config.rateLimitWindowMillis
    )

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        authRoutes(
            authService = authService,
            rateLimiter = rateLimiter
        )
    }
}
