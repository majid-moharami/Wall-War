package com.wallwar.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LoginScreen(
    authUiState: AuthUiState,
    isRegisterMode: Boolean,
    hasSavedSession: Boolean?,
    onLoginEmail: (String, String) -> Unit,
    onRegisterEmail: (String, String, String) -> Unit,
    onSignInWithGoogle: (android.content.Context) -> Unit,
    onContinueAsGuest: () -> Unit,
    onPlayAsGuestDevice: (android.content.Context) -> Unit = {},
    onToggleAuthMode: () -> Unit,
    onClearError: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthScreen(
        authUiState = authUiState,
        isRegisterMode = isRegisterMode,
        hasSavedSession = hasSavedSession,
        onLoginEmail = onLoginEmail,
        onRegisterEmail = onRegisterEmail,
        onSignInWithGoogle = onSignInWithGoogle,
        onContinueAsGuest = onContinueAsGuest,
        onPlayAsGuestDevice = onPlayAsGuestDevice,
        onToggleAuthMode = onToggleAuthMode,
        onClearError = onClearError,
        onAuthSuccess = onAuthSuccess,
        modifier = modifier
    )
}
