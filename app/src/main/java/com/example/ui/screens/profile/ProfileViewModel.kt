package com.example.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.SignInResult
import com.example.data.UserProfile
import com.example.data.nakama.NakamaFriend
import com.example.data.nakama.NakamaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val nakamaRepository: NakamaRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile
    val friends: StateFlow<List<NakamaFriend>> = nakamaRepository.friends

    private val _signInStatus = MutableStateFlow<String?>(null)
    val signInStatus: StateFlow<String?> = _signInStatus.asStateFlow()

    init {
        fetchFriends()
    }

    fun fetchFriends() {
        viewModelScope.launch {
            nakamaRepository.fetchFriends()
        }
    }

    fun addFriend(username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = nakamaRepository.addFriendByUsername(username)
            onResult(success)
        }
    }

    fun removeFriend(username: String) {
        viewModelScope.launch {
            nakamaRepository.removeFriend(username)
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _signInStatus.value = "Launching Google Sign-In..."
            val result = authRepository.signInWithGoogle(context)
            when (result) {
                is SignInResult.Success -> {
                    _signInStatus.value = "Signed in as ${result.name} (${result.email})"
                    fetchFriends()
                }
                is SignInResult.Cancelled -> {
                    _signInStatus.value = "Sign-in cancelled"
                }
                is SignInResult.Error -> {
                    _signInStatus.value = result.message
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
