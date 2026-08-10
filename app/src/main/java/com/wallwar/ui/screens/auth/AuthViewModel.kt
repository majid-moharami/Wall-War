package com.wallwar.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.data.AuthRepository
import com.wallwar.data.SignInResult
import com.wallwar.data.UserProfile
import com.wallwar.data.nakama.NakamaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val username: String, val email: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val nakamaRepository: NakamaRepository
) : ViewModel() {

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _isRegisterMode = MutableStateFlow(false)
    val isRegisterMode: StateFlow<Boolean> = _isRegisterMode.asStateFlow()

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    private val _hasSavedSession = MutableStateFlow<Boolean?>(null)
    val hasSavedSession: StateFlow<Boolean?> = _hasSavedSession.asStateFlow()

    init {
        checkSavedSession()
    }

    fun checkSavedSession() {
        viewModelScope.launch {
            if (nakamaRepository.hasValidSession()) {
                authRepository.syncFromNakamaServer()
                val current = authRepository.userProfile.value
                _hasSavedSession.value = true
                _authUiState.value = AuthUiState.Success(current.displayName, current.email)
            } else {
                _hasSavedSession.value = false
            }
        }
    }

    fun toggleAuthMode() {
        _isRegisterMode.value = !_isRegisterMode.value
        _authUiState.value = AuthUiState.Idle
    }

    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authUiState.value = AuthUiState.Error("Please enter both email and password.")
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.authenticateWithEmail(
                email = email.trim(),
                password = password.trim(),
                isRegister = false
            )
            when (result) {
                is SignInResult.Success -> {
                    _authUiState.value = AuthUiState.Success(result.name, result.email)
                }
                is SignInResult.Error -> {
                    _authUiState.value = AuthUiState.Error(result.message)
                }
                is SignInResult.Cancelled -> {
                    _authUiState.value = AuthUiState.Idle
                }
            }
        }
    }

    fun registerWithEmail(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank()) {
            _authUiState.value = AuthUiState.Error("Please enter email and password.")
            return
        }
        if (password.length < 8) {
            _authUiState.value = AuthUiState.Error("Password must be at least 8 characters long.")
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.authenticateWithEmail(
                email = email.trim(),
                password = password.trim(),
                isRegister = true,
                username = username.ifBlank { null }
            )
            when (result) {
                is SignInResult.Success -> {
                    _authUiState.value = AuthUiState.Success(result.name, result.email)
                }
                is SignInResult.Error -> {
                    _authUiState.value = AuthUiState.Error(result.message)
                }
                is SignInResult.Cancelled -> {
                    _authUiState.value = AuthUiState.Idle
                }
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.signInWithGoogle(context)
            when (result) {
                is SignInResult.Success -> {
                    _authUiState.value = AuthUiState.Success(result.name, result.email)
                }
                is SignInResult.Error -> {
                    _authUiState.value = AuthUiState.Error(result.message)
                }
                is SignInResult.Cancelled -> {
                    _authUiState.value = AuthUiState.Idle
                }
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val success = nakamaRepository.ensureAuthenticatedGuest(userProfile.value.displayName)
            if (success) {
                _authUiState.value = AuthUiState.Success(userProfile.value.displayName, "guest@wallwar.app")
            } else {
                _authUiState.value = AuthUiState.Error("Guest login failed. Please check server connection.")
            }
        }
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
            _hasSavedSession.value = false
            _authUiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        _authUiState.value = AuthUiState.Idle
    }
}
