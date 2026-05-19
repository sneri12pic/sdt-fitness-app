package com.stepandemianenko.sdtfitness.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepandemianenko.sdtfitness.Home
import com.stepandemianenko.sdtfitness.auth.domain.SessionState
import com.stepandemianenko.sdtfitness.auth.viewmodel.AuthUiEvent
import com.stepandemianenko.sdtfitness.auth.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class AuthGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            MaterialTheme {
                AuthGateRoute(
                    onAuthenticated = {
                        startActivity(Intent(this, Home::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AuthGateRoute(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val credentialAuthClient = remember(context) { CredentialAuthClient(context) }

    LaunchedEffect(uiState.sessionState) {
        when (uiState.sessionState) {
            is SessionState.Authenticated,
            SessionState.Guest -> onAuthenticated()

            SessionState.Checking,
            SessionState.SignedOut -> Unit
        }
    }

    when (uiState.sessionState) {
        SessionState.Checking -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        SessionState.SignedOut -> {
            val scope = rememberCoroutineScope()
            LoginScreen(
                uiState = uiState,
                onEmailChanged = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) },
                onPasswordChanged = { viewModel.onEvent(AuthUiEvent.PasswordChanged(it)) },
                onConfirmPasswordChanged = { viewModel.onEvent(AuthUiEvent.ConfirmPasswordChanged(it)) },
                onSubmitLoginClick = { viewModel.onEvent(AuthUiEvent.SubmitEmailLogin) },
                onSubmitRegistrationClick = { viewModel.onEvent(AuthUiEvent.SubmitRegistration) },
                onSwitchToSignInClick = { viewModel.onEvent(AuthUiEvent.SwitchToSignIn) },
                onSwitchToRegisterClick = { viewModel.onEvent(AuthUiEvent.SwitchToRegister) },
                onTogglePasswordVisibilityClick = { viewModel.onEvent(AuthUiEvent.TogglePasswordVisibility) },
                onToggleConfirmPasswordVisibilityClick = {
                    viewModel.onEvent(AuthUiEvent.ToggleConfirmPasswordVisibility)
                },
                onCredentialClick = {
                    scope.launch {
                        val credentialSecret = credentialAuthClient.requestPasswordCredential(context)
                        if (credentialSecret == null) {
                            viewModel.onEvent(AuthUiEvent.CredentialLoginFailed)
                        } else {
                            viewModel.onEvent(
                                AuthUiEvent.SavedPasswordReceived(
                                    email = credentialSecret.email,
                                    password = credentialSecret.password
                                )
                            )
                        }
                    }
                },
                onContinueAsGuestClick = { viewModel.onEvent(AuthUiEvent.ContinueAsGuest) }
            )
        }

        is SessionState.Authenticated,
        SessionState.Guest -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
