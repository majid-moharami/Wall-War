package com.example.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.SignInResult
import com.example.data.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    private val _signInStatus = MutableStateFlow<String?>(null)
    val signInStatus: StateFlow<String?> = _signInStatus.asStateFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _signInStatus.value = "Authenticating with Google & Firebase..."
            val result = authRepository.signInWithGoogle(context)
            when (result) {
                is SignInResult.Success -> {
                    _signInStatus.value = "Signed in as ${result.name} (${result.email})"
                }
                is SignInResult.Cancelled -> {
                    _signInStatus.value = "Sign-in cancelled by user"
                }
                is SignInResult.Error -> {
                    _signInStatus.value = "Authentication error: ${result.message}"
                }
            }
        }
    }

    fun clearSignInStatus() {
        _signInStatus.value = null
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
            _signInStatus.value = "Signed out successfully"
        }
    }
}
