package com.example.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    fun signInWithGoogle(context: Context, onFallbackRequired: () -> Unit) {
        viewModelScope.launch {
            val success = authRepository.signInWithGoogle(context)
            if (!success) {
                onFallbackRequired()
            }
        }
    }

    fun confirmGoogleAccount(name: String, email: String) {
        authRepository.signInWithGoogleAccountDetails(
            displayName = name,
            email = email,
            photoUrl = "https://lh3.googleusercontent.com/a/default-user"
        )
    }

    fun signOut() {
        authRepository.signOut()
    }
}
