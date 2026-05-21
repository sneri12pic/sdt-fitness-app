package com.stepandemianenko.sdtfitness.authapi.data

import com.stepandemianenko.sdtfitness.authapi.domain.AuthUser
import com.stepandemianenko.sdtfitness.authapi.domain.DuplicateAccountException
import com.stepandemianenko.sdtfitness.authapi.domain.PASSWORD_AUTH_PROVIDER
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.UUID
import javax.sql.DataSource

class JdbcAuthRepository(
    private val dataSource: DataSource
) : AuthRepository {

    override fun findUserByEmail(email: String): AuthUser? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, remote_user_id, email, display_name, auth_provider, password_hash
                FROM auth_users
                WHERE email = ?
                LIMIT 1
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, email)
                statement.executeQuery().use { results ->
                    return if (results.next()) results.toAuthUser() else null
                }
            }
        }
    }

    override fun createPasswordUser(email: String, passwordHash: String, nowMillis: Long): AuthUser {
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement(
                    """
                    INSERT INTO auth_users (
                        remote_user_id,
                        email,
                        display_name,
                        auth_provider,
                        password_hash,
                        created_at_millis,
                        updated_at_millis
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    Statement.RETURN_GENERATED_KEYS
                ).use { statement ->
                    statement.setString(1, UUID.randomUUID().toString())
                    statement.setString(2, email)
                    statement.setString(3, null)
                    statement.setString(4, PASSWORD_AUTH_PROVIDER)
                    statement.setString(5, passwordHash)
                    statement.setLong(6, nowMillis)
                    statement.setLong(7, nowMillis)
                    statement.executeUpdate()
                    statement.generatedKeys.use { keys ->
                        if (!keys.next()) error("No generated user id returned.")
                    }
                }
            }
        } catch (exception: SQLException) {
            if (exception.isUniqueViolation()) {
                throw DuplicateAccountException()
            }
            throw exception
        }

        return findUserByEmail(email) ?: error("Created user could not be loaded.")
    }

    override fun updateLastLogin(userId: Long, nowMillis: Long) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE auth_users
                SET last_login_at_millis = ?, updated_at_millis = ?
                WHERE id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, nowMillis)
                statement.setLong(2, nowMillis)
                statement.setLong(3, userId)
                statement.executeUpdate()
            }
        }
    }

    override fun createRefreshToken(
        userId: Long,
        tokenHash: String,
        expiresAtMillis: Long,
        nowMillis: Long
    ) {
        dataSource.connection.use { connection ->
            insertRefreshToken(connection, userId, tokenHash, expiresAtMillis, nowMillis)
        }
    }

    override fun rotateRefreshToken(
        oldTokenHash: String,
        newTokenHash: String,
        newExpiresAtMillis: Long,
        nowMillis: Long
    ): AuthUser? {
        return dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val row = loadRefreshTokenForUpdate(connection, oldTokenHash)
                if (row == null) {
                    connection.rollback()
                    return@use null
                }

                if (row.revokedAtMillis != null || row.expiresAtMillis <= nowMillis) {
                    revokeAllRefreshTokens(connection, row.user.id, nowMillis)
                    connection.commit()
                    return@use null
                }

                connection.prepareStatement(
                    """
                    UPDATE refresh_tokens
                    SET revoked_at_millis = ?,
                        replaced_by_token_hash = ?,
                        last_used_at_millis = ?
                    WHERE id = ? AND revoked_at_millis IS NULL
                    """.trimIndent()
                ).use { statement ->
                    statement.setLong(1, nowMillis)
                    statement.setString(2, newTokenHash)
                    statement.setLong(3, nowMillis)
                    statement.setLong(4, row.tokenId)
                    if (statement.executeUpdate() != 1) {
                        connection.rollback()
                        return@use null
                    }
                }

                insertRefreshToken(connection, row.user.id, newTokenHash, newExpiresAtMillis, nowMillis)
                connection.commit()
                row.user
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            } finally {
                connection.autoCommit = true
            }
        }
    }

    override fun revokeAllRefreshTokens(userId: Long, nowMillis: Long) {
        dataSource.connection.use { connection ->
            revokeAllRefreshTokens(connection, userId, nowMillis)
        }
    }

    private fun loadRefreshTokenForUpdate(connection: Connection, tokenHash: String): RefreshTokenRow? {
        connection.prepareStatement(
            """
            SELECT rt.id AS refresh_token_id,
                   rt.expires_at_millis,
                   rt.revoked_at_millis,
                   u.id,
                   u.remote_user_id,
                   u.email,
                   u.display_name,
                   u.auth_provider,
                   u.password_hash
            FROM refresh_tokens rt
            JOIN auth_users u ON u.id = rt.user_id
            WHERE rt.token_hash = ?
            LIMIT 1
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, tokenHash)
            statement.executeQuery().use { results ->
                if (!results.next()) return null
                return RefreshTokenRow(
                    tokenId = results.getLong("refresh_token_id"),
                    expiresAtMillis = results.getLong("expires_at_millis"),
                    revokedAtMillis = results.getNullableLong("revoked_at_millis"),
                    user = results.toAuthUser()
                )
            }
        }
    }

    private fun insertRefreshToken(
        connection: Connection,
        userId: Long,
        tokenHash: String,
        expiresAtMillis: Long,
        nowMillis: Long
    ) {
        connection.prepareStatement(
            """
            INSERT INTO refresh_tokens (
                user_id,
                token_hash,
                expires_at_millis,
                created_at_millis
            )
            VALUES (?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, userId)
            statement.setString(2, tokenHash)
            statement.setLong(3, expiresAtMillis)
            statement.setLong(4, nowMillis)
            statement.executeUpdate()
        }
    }

    private fun revokeAllRefreshTokens(connection: Connection, userId: Long, nowMillis: Long) {
        connection.prepareStatement(
            """
            UPDATE refresh_tokens
            SET revoked_at_millis = ?
            WHERE user_id = ? AND revoked_at_millis IS NULL
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, nowMillis)
            statement.setLong(2, userId)
            statement.executeUpdate()
        }
    }

    private fun ResultSet.toAuthUser(): AuthUser {
        return AuthUser(
            id = getLong("id"),
            remoteUserId = getString("remote_user_id"),
            email = getString("email"),
            displayName = getString("display_name"),
            authProvider = getString("auth_provider"),
            passwordHash = getString("password_hash")
        )
    }

    private fun ResultSet.getNullableLong(column: String): Long? {
        val value = getLong(column)
        return if (wasNull()) null else value
    }

    private fun SQLException.isUniqueViolation(): Boolean {
        return sqlState == "23505" || message.orEmpty().contains("unique", ignoreCase = true)
    }

    private data class RefreshTokenRow(
        val tokenId: Long,
        val expiresAtMillis: Long,
        val revokedAtMillis: Long?,
        val user: AuthUser
    )
}
