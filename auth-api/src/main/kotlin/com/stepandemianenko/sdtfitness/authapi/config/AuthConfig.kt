package com.stepandemianenko.sdtfitness.authapi.config

data class AuthConfig(
    val port: Int,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtSecret: String,
    val refreshTokenPepper: String,
    val accessTokenTtlMillis: Long,
    val refreshTokenTtlMillis: Long,
    val bcryptCost: Int,
    val rateLimitMaxRequests: Int,
    val rateLimitWindowMillis: Long,
    val allowedOrigins: List<String>,
    val environment: String
) {
    val isProduction: Boolean = environment.equals("production", ignoreCase = true)

    init {
        require(jwtSecret.length >= MIN_SECRET_LENGTH) {
            "AUTH_JWT_SECRET must be at least $MIN_SECRET_LENGTH characters."
        }
        require(refreshTokenPepper.length >= MIN_SECRET_LENGTH) {
            "AUTH_REFRESH_TOKEN_PEPPER must be at least $MIN_SECRET_LENGTH characters."
        }
        require(bcryptCost in 4..16) {
            "AUTH_BCRYPT_COST must be between 4 and 16."
        }
    }

    companion object {
        private const val MIN_SECRET_LENGTH = 32
        private const val DEFAULT_DEV_SECRET = "dev-only-secret-change-before-production-32"
        private const val DEFAULT_DEV_PEPPER = "dev-only-refresh-pepper-change-before-prod"
        private const val FIFTEEN_MINUTES = 15 * 60 * 1000L
        private const val THIRTY_DAYS = 30L * 24L * 60L * 60L * 1000L

        fun fromEnvironment(env: Map<String, String> = System.getenv()): AuthConfig {
            val environment = env["AUTH_ENVIRONMENT"] ?: env["ENVIRONMENT"] ?: "development"
            val production = environment.equals("production", ignoreCase = true)
            val jwtSecret = env["AUTH_JWT_SECRET"] ?: DEFAULT_DEV_SECRET
            val refreshPepper = env["AUTH_REFRESH_TOKEN_PEPPER"] ?: DEFAULT_DEV_PEPPER

            if (production) {
                require(env["AUTH_JWT_SECRET"] != null) {
                    "AUTH_JWT_SECRET is required in production."
                }
                require(env["AUTH_REFRESH_TOKEN_PEPPER"] != null) {
                    "AUTH_REFRESH_TOKEN_PEPPER is required in production."
                }
            }

            return AuthConfig(
                port = env["PORT"]?.toIntOrNull() ?: env["AUTH_PORT"]?.toIntOrNull() ?: 8080,
                databaseUrl = env["AUTH_DATABASE_URL"]
                    ?: "jdbc:h2:file:./build/auth-api/dev-db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                databaseUser = env["AUTH_DATABASE_USER"] ?: "sa",
                databasePassword = env["AUTH_DATABASE_PASSWORD"] ?: "",
                jwtIssuer = env["AUTH_JWT_ISSUER"] ?: "sdt-fitness-auth-api",
                jwtAudience = env["AUTH_JWT_AUDIENCE"] ?: "sdt-fitness-android",
                jwtSecret = jwtSecret,
                refreshTokenPepper = refreshPepper,
                accessTokenTtlMillis = env["AUTH_ACCESS_TOKEN_TTL_MILLIS"]?.toLongOrNull()
                    ?: FIFTEEN_MINUTES,
                refreshTokenTtlMillis = env["AUTH_REFRESH_TOKEN_TTL_MILLIS"]?.toLongOrNull()
                    ?: THIRTY_DAYS,
                bcryptCost = env["AUTH_BCRYPT_COST"]?.toIntOrNull() ?: 12,
                rateLimitMaxRequests = env["AUTH_RATE_LIMIT_MAX_REQUESTS"]?.toIntOrNull() ?: 20,
                rateLimitWindowMillis = env["AUTH_RATE_LIMIT_WINDOW_MILLIS"]?.toLongOrNull() ?: 60_000L,
                allowedOrigins = env["AUTH_ALLOWED_ORIGINS"]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList(),
                environment = environment
            )
        }
    }
}
