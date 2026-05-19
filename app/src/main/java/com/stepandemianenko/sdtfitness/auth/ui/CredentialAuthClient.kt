package com.stepandemianenko.sdtfitness.auth.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential

data class SavedPasswordCredential(
    val email: String,
    val password: String
)

class CredentialAuthClient(
    context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun requestPasswordCredential(context: Context): SavedPasswordCredential? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetPasswordOption())
            .build()

        val credential = runCatching {
            credentialManager.getCredential(
                context = context,
                request = request
            ).credential
        }.getOrNull()

        val passwordCredential = credential as? PasswordCredential ?: return null
        return SavedPasswordCredential(
            email = passwordCredential.id,
            password = passwordCredential.password
        )
    }
}
