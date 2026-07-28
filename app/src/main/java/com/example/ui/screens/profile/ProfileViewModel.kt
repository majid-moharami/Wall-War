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

    private val _showAccountChooser = MutableStateFlow(false)
    val showAccountChooser: StateFlow<Boolean> = _showAccountChooser.asStateFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _signInStatus.value = "Connecting to Google Sign-In..."
            val result = authRepository.signInWithGoogle(context)
            when (result) {
                is SignInResult.Success -> {
                    _signInStatus.value = "Signed in successfully as ${result.name}"
                    _showAccountChooser.value = false
                }
                is SignInResult.Cancelled -> {
                    _signInStatus.value = "Sign-in cancelled by user"
                    _showAccountChooser.value = false
                }
                is SignInResult.RequiresAccountChooser -> {
                    _signInStatus.value = null
                    _showAccountChooser.value = true
                }
                is SignInResult.Error -> {
                    _signInStatus.value = "Sign-in error: ${result.message}"
                    _showAccountChooser.value = false
                }
            }
        }
    }

    fun confirmGoogleAccount(name: String, email: String) {
        authRepository.signInWithGoogleAccountDetails(
            displayName = name,
            email = email,
            photoUrl = "https://lh3.googleusercontent.com/a/default-user"
        )
        _showAccountChooser.value = false
        _signInStatus.value = "Signed in with Google as $name ($email)"
    }

    fun dismissAccountChooser() {
        _showAccountChooser.value = false
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
