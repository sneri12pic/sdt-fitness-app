package com.stepandemianenko.sdtfitness.auth.data

import com.stepandemianenko.sdtfitness.auth.domain.AuthFailureReason
import com.stepandemianenko.sdtfitness.auth.domain.AuthRepository
import com.stepandemianenko.sdtfitness.auth.domain.AuthResult
import com.stepandemianenko.sdtfitness.auth.domain.AuthenticatedUser
import com.stepandemianenko.sdtfitness.auth.domain.SessionState
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val secureSessionStore: SecureSessionStore,
    private val accountSessionManager: AccountSessionManager
) : AuthRepository {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var accessToken: String? = null

    override suspend fun restoreSession() {
        val storedSession = secureSessionStore.load()
        if (storedSession == null) {
            _sessionState.value = SessionState.SignedOut
            return
        }

        val refreshed = runCatching {
            remoteAuthDataSource.refresh(storedSession.refreshToken)
        }.getOrElse {
            secureSessionStore.clear()
            _sessionState.value = SessionState.SignedOut
            return
        }
        completeAuthenticatedSession(refreshed)
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            return AuthResult.Failure(AuthFailureReason.InvalidInput)
        }

        return runCatching {
            remoteAuthDataSource.signInWithEmail(
                email = normalizedEmail,
                password = password
            )
        }.fold(
            onSuccess = { response ->
                completeAuthenticatedSession(response)
                AuthResult.Success
            },
            onFailure = { AuthResult.Failure(it.toFailureReason()) }
        )
    }

    override suspend fun registerWithEmail(email: String, password: String): AuthResult {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            return AuthResult.Failure(AuthFailureReason.InvalidInput)
        }

        return runCatching {
            remoteAuthDataSource.registerWithEmail(
                email = normalizedEmail,
                password = password
            )
        }.fold(
            onSuccess = { response ->
                completeAuthenticatedSession(response)
                AuthResult.Success
            },
            onFailure = { AuthResult.Failure(it.toFailureReason()) }
        )
    }

    override suspend fun signInWithCredential(credentialToken: String): AuthResult {
        if (credentialToken.isBlank()) {
            return AuthResult.Failure(AuthFailureReason.InvalidInput)
        }

        return runCatching {
            remoteAuthDataSource.signInWithCredential(credentialToken)
        }.fold(
            onSuccess = { response ->
                completeAuthenticatedSession(response)
                AuthResult.Success
            },
            onFailure = { AuthResult.Failure(it.toFailureReason()) }
        )
    }

    override suspend fun continueAsGuest(): AuthResult {
        accountSessionManager.switchToGuestAccount()
        _sessionState.value = SessionState.Guest
        return AuthResult.Success
    }

    override suspend fun signOut() {
        accessToken = null
        secureSessionStore.clear()
        _sessionState.value = SessionState.SignedOut
    }

    private suspend fun completeAuthenticatedSession(response: RemoteAuthResponse) {
        accessToken = response.accessToken
        secureSessionStore.save(
            StoredSession(
                remoteUserId = response.remoteUserId,
                email = response.email,
                displayName = response.displayName,
                authProvider = response.authProvider,
                refreshToken = response.refreshToken,
                accessTokenExpiresAtMillis = response.accessTokenExpiresAtMillis
            )
        )
        val accountId = accountSessionManager.linkAuthenticatedAccountAndSwitch(
            remoteUserId = response.remoteUserId,
            email = response.email,
            displayName = response.displayName,
            authProvider = response.authProvider
        )
        _sessionState.value = SessionState.Authenticated(
            AuthenticatedUser(
                accountId = accountId,
                remoteUserId = response.remoteUserId,
                email = response.email,
                displayName = response.displayName,
                authProvider = response.authProvider
            )
        )
    }

    private fun Throwable.toFailureReason(): AuthFailureReason {
        return when (this) {
            RemoteAuthException.AccountAlreadyExists -> AuthFailureReason.AccountAlreadyExists
            RemoteAuthException.InvalidCredentials -> AuthFailureReason.InvalidCredentials
            RemoteAuthException.Network -> AuthFailureReason.Network
            RemoteAuthException.ServiceUnavailable -> AuthFailureReason.ServiceUnavailable
            is RemoteAuthException.Unexpected -> AuthFailureReason.ServiceUnavailable
            else -> AuthFailureReason.Unknown
        }
    }
}
