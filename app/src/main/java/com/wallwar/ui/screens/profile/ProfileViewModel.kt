package com.wallwar.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.data.AuthRepository
import com.wallwar.data.SignInResult
import com.wallwar.data.UserProfile
import com.wallwar.data.nakama.NakamaFriend
import com.wallwar.data.nakama.NakamaRepository
import com.wallwar.data.ProfileSkin
import com.wallwar.data.ProfileSkinCatalog
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
    val unlockedAvatarSkinIds: StateFlow<Set<String>> = authRepository.unlockedAvatarSkinIds

    val allProfileSkins: List<ProfileSkin> = ProfileSkinCatalog.ALL_SKINS
    val savedGooglePhotoUrl: String? get() = authRepository.getSavedGooglePhotoUrl()

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

    fun unlockAvatarSkin(skin: ProfileSkin, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentCoins = userProfile.value.coins
            if (currentCoins < skin.priceCoins) {
                onResult(false, "Not enough coins! You need ${skin.priceCoins} 🪙.")
                return@launch
            }

            val success = authRepository.unlockAvatarSkin(skin.id, skin.priceCoins)
            if (success) {
                authRepository.equipAvatarSkin(skin.id)
                _signInStatus.value = "Unlocked & Equipped ${skin.name}!"
                onResult(true, "Successfully unlocked & equipped ${skin.name}!")
            } else {
                onResult(false, "Purchase failed.")
            }
        }
    }

    fun equipAvatarSkin(skin: ProfileSkin) {
        viewModelScope.launch {
            authRepository.equipAvatarSkin(skin.id)
            _signInStatus.value = "Equipped ${skin.name} profile skin"
        }
    }

    fun equipGoogleAvatar() {
        viewModelScope.launch {
            val googleUrl = authRepository.getSavedGooglePhotoUrl()
            if (!googleUrl.isNullOrBlank()) {
                authRepository.equipGoogleAvatar(googleUrl)
                _signInStatus.value = "Equipped Google Profile Photo"
            }
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
