package com.stepandemianenko.sdtfitness.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepandemianenko.sdtfitness.auth.domain.AuthFailureReason
import com.stepandemianenko.sdtfitness.auth.domain.AuthResult
import com.stepandemianenko.sdtfitness.auth.domain.SessionState
import com.stepandemianenko.sdtfitness.data.AppGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val mode: AuthMode = AuthMode.SignIn,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sessionState: SessionState = SessionState.Checking
)

enum class AuthMode {
    SignIn,
    Register
}

sealed interface AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    data class ConfirmPasswordChanged(val value: String) : AuthUiEvent
    data object SwitchToSignIn : AuthUiEvent
    data object SwitchToRegister : AuthUiEvent
    data object TogglePasswordVisibility : AuthUiEvent
    data object ToggleConfirmPasswordVisibility : AuthUiEvent
    data object SubmitEmailLogin : AuthUiEvent
    data object SubmitRegistration : AuthUiEvent
    data object ContinueAsGuest : AuthUiEvent
    data class SavedPasswordReceived(val email: String, val password: String) : AuthUiEvent
    data class CredentialTokenReceived(val token: String) : AuthUiEvent
    data object CredentialLoginFailed : AuthUiEvent
}

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val authRepository = AppGraph.authRepository(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collect { sessionState ->
                _uiState.update {
                    it.copy(
                        sessionState = sessionState,
                        isLoading = sessionState == SessionState.Checking
                    )
                }
            }
        }
        viewModelScope.launch {
            authRepository.restoreSession()
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> {
                _uiState.update {
                    it.copy(email = event.value.trim(), errorMessage = null)
                }
            }

            is AuthUiEvent.PasswordChanged -> {
                _uiState.update {
                    it.copy(password = event.value, errorMessage = null)
                }
            }

            is AuthUiEvent.ConfirmPasswordChanged -> {
                _uiState.update {
                    it.copy(confirmPassword = event.value, errorMessage = null)
                }
            }

            AuthUiEvent.SwitchToSignIn -> {
                _uiState.update {
                    it.copy(
                        mode = AuthMode.SignIn,
                        password = "",
                        confirmPassword = "",
                        errorMessage = null,
                        isPasswordVisible = false,
                        isConfirmPasswordVisible = false
                    )
                }
            }

            AuthUiEvent.SwitchToRegister -> {
                _uiState.update {
                    it.copy(
                        mode = AuthMode.Register,
                        password = "",
                        confirmPassword = "",
                        errorMessage = null,
                        isPasswordVisible = false,
                        isConfirmPasswordVisible = false
                    )
                }
            }

            AuthUiEvent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            AuthUiEvent.ToggleConfirmPasswordVisibility -> {
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }

            AuthUiEvent.SubmitEmailLogin -> signInWithEmail()
            AuthUiEvent.SubmitRegistration -> registerWithEmail()
            AuthUiEvent.ContinueAsGuest -> continueAsGuest()
            is AuthUiEvent.SavedPasswordReceived -> signInWithEmail(event.email, event.password)
            is AuthUiEvent.CredentialTokenReceived -> signInWithCredential(event.token)
            AuthUiEvent.CredentialLoginFailed -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = GENERIC_AUTH_ERROR
                    )
                }
            }
        }
    }

    private fun signInWithEmail(
        email: String = _uiState.value.email,
        password: String = _uiState.value.password
    ) {
        when (val validation = AuthFormValidator.validateLogin(email, password)) {
            is AuthFormValidationResult.Invalid -> {
                _uiState.update { it.copy(errorMessage = validation.message) }
                return
            }

            AuthFormValidationResult.Valid -> Unit
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithEmail(
                email = email,
                password = password
            )
            handleResult(result)
        }
    }

    private fun registerWithEmail() {
        val snapshot = _uiState.value
        when (
            val validation = AuthFormValidator.validateRegistration(
                email = snapshot.email,
                password = snapshot.password,
                confirmPassword = snapshot.confirmPassword
            )
        ) {
            is AuthFormValidationResult.Invalid -> {
                _uiState.update { it.copy(errorMessage = validation.message) }
                return
            }

            AuthFormValidationResult.Valid -> Unit
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.registerWithEmail(
                email = snapshot.email,
                password = snapshot.password
            )
            handleResult(result, genericErrorMessage = GENERIC_REGISTRATION_ERROR)
        }
    }

    private fun signInWithCredential(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            handleResult(authRepository.signInWithCredential(token))
        }
    }

    private fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            handleResult(authRepository.continueAsGuest())
        }
    }

    private fun handleResult(
        result: AuthResult,
        genericErrorMessage: String = GENERIC_AUTH_ERROR
    ) {
        when (result) {
            AuthResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        password = "",
                        confirmPassword = "",
                        errorMessage = null,
                        isPasswordVisible = false,
                        isConfirmPasswordVisible = false
                    )
                }
            }

            is AuthResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        password = "",
                        confirmPassword = "",
                        isPasswordVisible = false,
                        isConfirmPasswordVisible = false,
                        errorMessage = result.reason.toUiMessage(
                            genericErrorMessage = genericErrorMessage
                        )
                    )
                }
            }
        }
    }

    private fun AuthFailureReason.toUiMessage(genericErrorMessage: String): String {
        return when (this) {
            AuthFailureReason.InvalidInput -> "Enter your email and password."
            AuthFailureReason.AccountAlreadyExists -> "An account with this email already exists."
            AuthFailureReason.ServiceUnavailable -> {
                if (genericErrorMessage == GENERIC_REGISTRATION_ERROR) {
                    "Registration is not available right now."
                } else {
                    "Sign in is not available right now."
                }
            }

            AuthFailureReason.InvalidCredentials,
            AuthFailureReason.Network,
            AuthFailureReason.Unknown -> genericErrorMessage
        }
    }

    private companion object {
        const val GENERIC_AUTH_ERROR = "We could not sign you in. Check your details and try again."
        const val GENERIC_REGISTRATION_ERROR = "We could not create your account. Try again later."
    }
}
