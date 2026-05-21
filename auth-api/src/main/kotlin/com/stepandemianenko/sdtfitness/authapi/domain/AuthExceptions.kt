package com.stepandemianenko.sdtfitness.authapi.domain

import io.ktor.http.HttpStatusCode

sealed class AuthApiException(
    val statusCode: HttpStatusCode,
    val safeErrorCode: String
) : RuntimeException(safeErrorCode)

class ValidationException : AuthApiException(HttpStatusCode.BadRequest, "invalid_request")

class DuplicateAccountException : AuthApiException(HttpStatusCode.Conflict, "account_already_exists")

class InvalidCredentialsException : AuthApiException(HttpStatusCode.Unauthorized, "invalid_credentials")

class RateLimitException : AuthApiException(HttpStatusCode.TooManyRequests, "rate_limited")

class HttpsRequiredException : AuthApiException(HttpStatusCode.Forbidden, "https_required")
