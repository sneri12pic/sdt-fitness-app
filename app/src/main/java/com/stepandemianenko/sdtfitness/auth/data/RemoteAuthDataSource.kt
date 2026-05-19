package com.stepandemianenko.sdtfitness.auth.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class RemoteAuthResponse(
    val remoteUserId: String,
    val email: String,
    val displayName: String?,
    val authProvider: String,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtMillis: Long
)

sealed class RemoteAuthException : Exception() {
    data object ServiceUnavailable : RemoteAuthException()
    data object AccountAlreadyExists : RemoteAuthException()
    data object InvalidCredentials : RemoteAuthException()
    data object Network : RemoteAuthException()
    data class Unexpected(val statusCode: Int? = null) : RemoteAuthException()
}

interface RemoteAuthDataSource {
    suspend fun signInWithEmail(email: String, password: String): RemoteAuthResponse
    suspend fun registerWithEmail(email: String, password: String): RemoteAuthResponse
    suspend fun signInWithCredential(credentialToken: String): RemoteAuthResponse
    suspend fun refresh(refreshToken: String): RemoteAuthResponse
}

class HttpRemoteAuthDataSource(
    private val baseUrl: String
) : RemoteAuthDataSource {

    override suspend fun signInWithEmail(email: String, password: String): RemoteAuthResponse {
        return postJson(
            path = "/auth/login",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
        )
    }

    override suspend fun registerWithEmail(email: String, password: String): RemoteAuthResponse {
        return postJson(
            path = "/auth/register",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
        )
    }

    override suspend fun signInWithCredential(credentialToken: String): RemoteAuthResponse {
        return postJson(
            path = "/auth/credential",
            body = JSONObject().put("credentialToken", credentialToken)
        )
    }

    override suspend fun refresh(refreshToken: String): RemoteAuthResponse {
        return postJson(
            path = "/auth/refresh",
            body = JSONObject().put("refreshToken", refreshToken)
        )
    }

    private suspend fun postJson(path: String, body: JSONObject): RemoteAuthResponse = withContext(Dispatchers.IO) {
        val trimmedBaseUrl = baseUrl.trim().trimEnd('/')
        if (trimmedBaseUrl.isBlank() || !trimmedBaseUrl.startsWith("https://")) {
            throw RemoteAuthException.ServiceUnavailable
        }

        val connection = try {
            (URL("$trimmedBaseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
        } catch (_: Exception) {
            throw RemoteAuthException.Network
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }

            val statusCode = connection.responseCode
            if (statusCode == HttpURLConnection.HTTP_CONFLICT) {
                throw RemoteAuthException.AccountAlreadyExists
            }
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                throw RemoteAuthException.InvalidCredentials
            }
            if (statusCode !in 200..299) {
                throw RemoteAuthException.Unexpected(statusCode)
            }

            val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseResponse(JSONObject(responseText))
        } catch (exception: RemoteAuthException) {
            throw exception
        } catch (_: java.io.IOException) {
            throw RemoteAuthException.Network
        } catch (_: Exception) {
            throw RemoteAuthException.Unexpected()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(json: JSONObject): RemoteAuthResponse {
        return RemoteAuthResponse(
            remoteUserId = json.getString("remoteUserId"),
            email = json.getString("email"),
            displayName = json.optString("displayName").takeIf { it.isNotBlank() },
            authProvider = json.optString("authProvider", "password"),
            accessToken = json.getString("accessToken"),
            refreshToken = json.getString("refreshToken"),
            accessTokenExpiresAtMillis = json.optLong(
                "accessTokenExpiresAtMillis",
                System.currentTimeMillis() + DEFAULT_ACCESS_TOKEN_TTL_MILLIS
            )
        )
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
        const val DEFAULT_ACCESS_TOKEN_TTL_MILLIS = 15 * 60 * 1000L
    }
}
