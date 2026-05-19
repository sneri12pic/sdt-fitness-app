package com.stepandemianenko.sdtfitness.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepandemianenko.sdtfitness.auth.viewmodel.AuthMode
import com.stepandemianenko.sdtfitness.auth.viewmodel.AuthUiState

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSubmitLoginClick: () -> Unit,
    onSubmitRegistrationClick: () -> Unit,
    onSwitchToSignInClick: () -> Unit,
    onSwitchToRegisterClick: () -> Unit,
    onTogglePasswordVisibilityClick: () -> Unit,
    onToggleConfirmPasswordVisibilityClick: () -> Unit,
    onCredentialClick: () -> Unit,
    onContinueAsGuestClick: () -> Unit
) {
    val isRegisterMode = uiState.mode == AuthMode.Register
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AuthBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SDT Fitness",
                    color = AuthPrimaryText,
                    fontSize = 36.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRegisterMode) {
                        "Create an account to sync your workouts."
                    } else {
                        "Sign in to sync your workouts."
                    },
                    color = AuthSecondaryText,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    enabled = !uiState.isLoading,
                    isError = uiState.errorMessage != null,
                    colors = authTextFieldColors()
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { confirmPasswordFocusRequester.requestFocus() },
                        onDone = {
                            focusManager.clearFocus()
                            onSubmitLoginClick()
                        }
                    ),
                    trailingIcon = {
                        PasswordVisibilityButton(
                            visible = uiState.isPasswordVisible,
                            onClick = onTogglePasswordVisibilityClick
                        )
                    },
                    enabled = !uiState.isLoading,
                    isError = uiState.errorMessage != null,
                    colors = authTextFieldColors()
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(confirmPasswordFocusRequester),
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = if (uiState.isConfirmPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onSubmitRegistrationClick()
                            }
                        ),
                        trailingIcon = {
                            PasswordVisibilityButton(
                                visible = uiState.isConfirmPasswordVisible,
                                onClick = onToggleConfirmPasswordVisibilityClick
                            )
                        },
                        enabled = !uiState.isLoading,
                        isError = uiState.errorMessage != null,
                        colors = authTextFieldColors()
                    )
                }

                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = AuthError,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                }

                Button(
                    onClick = if (isRegisterMode) onSubmitRegistrationClick else onSubmitLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuthAction,
                        contentColor = AuthActionText
                    ),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = AuthActionText,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "Register" else "Sign in",
                            fontSize = 18.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                TextButton(
                    onClick = if (isRegisterMode) onSwitchToSignInClick else onSwitchToRegisterClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = if (isRegisterMode) {
                            "Already have an account? Sign in"
                        } else {
                            "Need an account? Register"
                        },
                        color = AuthPrimaryText
                    )
                }

                if (!isRegisterMode) {
                    TextButton(
                        onClick = onCredentialClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Text("Use saved credential", color = AuthPrimaryText)
                    }
                }

                TextButton(
                    onClick = onContinueAsGuestClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Continue as guest", color = AuthSecondaryText)
                        Text(
                            text = "Use the app locally without sync.",
                            color = AuthSecondaryText.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordVisibilityButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Hide password" else "Show password",
            tint = AuthSecondaryText
        )
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AuthPrimaryText,
    unfocusedTextColor = AuthPrimaryText,
    disabledTextColor = AuthPrimaryText.copy(alpha = 0.45f),
    errorTextColor = AuthPrimaryText,
    focusedContainerColor = AuthInputContainer,
    unfocusedContainerColor = AuthInputContainer,
    disabledContainerColor = AuthInputContainer.copy(alpha = 0.65f),
    errorContainerColor = AuthInputContainer,
    cursorColor = AuthAction,
    errorCursorColor = AuthError,
    focusedBorderColor = AuthAction,
    unfocusedBorderColor = AuthSecondaryText.copy(alpha = 0.45f),
    disabledBorderColor = AuthSecondaryText.copy(alpha = 0.25f),
    errorBorderColor = AuthError,
    focusedLabelColor = AuthAction,
    unfocusedLabelColor = AuthSecondaryText,
    disabledLabelColor = AuthSecondaryText.copy(alpha = 0.5f),
    errorLabelColor = AuthError,
    focusedTrailingIconColor = AuthSecondaryText,
    unfocusedTrailingIconColor = AuthSecondaryText,
    disabledTrailingIconColor = AuthSecondaryText.copy(alpha = 0.45f),
    errorTrailingIconColor = AuthError
)

private val AuthBackground = Color(0xFFEBC0B0)
private val AuthPrimaryText = Color(0xFF4F2912)
private val AuthSecondaryText = Color(0xFF6B4637)
private val AuthAction = Color(0xFFF27F3E)
private val AuthActionText = Color(0xFFFFF2E9)
private val AuthError = Color(0xFF9A2E1F)
private val AuthInputContainer = Color(0xFFFFEFE5)

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            uiState = AuthUiState(),
            onEmailChanged = {},
            onPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onSubmitLoginClick = {},
            onSubmitRegistrationClick = {},
            onSwitchToSignInClick = {},
            onSwitchToRegisterClick = {},
            onTogglePasswordVisibilityClick = {},
            onToggleConfirmPasswordVisibilityClick = {},
            onCredentialClick = {},
            onContinueAsGuestClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun RegisterScreenPreview() {
    MaterialTheme {
        LoginScreen(
            uiState = AuthUiState(mode = AuthMode.Register),
            onEmailChanged = {},
            onPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onSubmitLoginClick = {},
            onSubmitRegistrationClick = {},
            onSwitchToSignInClick = {},
            onSwitchToRegisterClick = {},
            onTogglePasswordVisibilityClick = {},
            onToggleConfirmPasswordVisibilityClick = {},
            onCredentialClick = {},
            onContinueAsGuestClick = {}
        )
    }
}
