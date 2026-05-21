package com.stepandemianenko.sdtfitness.authapi.routing

import com.stepandemianenko.sdtfitness.authapi.domain.CredentialRequest
import com.stepandemianenko.sdtfitness.authapi.domain.EmailPasswordRequest
import com.stepandemianenko.sdtfitness.authapi.domain.RefreshRequest
import com.stepandemianenko.sdtfitness.authapi.rate.FixedWindowRateLimiter
import com.stepandemianenko.sdtfitness.authapi.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(
    authService: AuthService,
    rateLimiter: FixedWindowRateLimiter
) {
    route("/auth") {
        post("/register") {
            call.requireRateLimit(rateLimiter)
            val request = call.receive<EmailPasswordRequest>()
            call.respond(HttpStatusCode.OK, authService.register(request.email, request.password))
        }

        post("/login") {
            call.requireRateLimit(rateLimiter)
            val request = call.receive<EmailPasswordRequest>()
            call.respond(HttpStatusCode.OK, authService.login(request.email, request.password))
        }

        post("/refresh") {
            call.requireRateLimit(rateLimiter)
            val request = call.receive<RefreshRequest>()
            call.respond(HttpStatusCode.OK, authService.refresh(request.refreshToken))
        }

        post("/credential") {
            call.requireRateLimit(rateLimiter)
            val request = call.receive<CredentialRequest>()
            call.respond(HttpStatusCode.OK, authService.signInWithCredential(request.credentialToken))
        }
    }
}

private fun ApplicationCall.requireRateLimit(rateLimiter: FixedWindowRateLimiter) {
    val client = request.origin.remoteHost
    val endpoint = request.uri
    rateLimiter.requireAllowed("$client:$endpoint")
}
