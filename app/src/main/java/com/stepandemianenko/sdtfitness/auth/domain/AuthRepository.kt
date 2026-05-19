package com.stepandemianenko.sdtfitness.auth.domain

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun restoreSession()
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun registerWithEmail(email: String, password: String): AuthResult
    suspend fun signInWithCredential(credentialToken: String): AuthResult
    suspend fun continueAsGuest(): AuthResult
    suspend fun signOut()
}
