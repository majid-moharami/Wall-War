package com.wallwar.ui.screens.emoji

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.EmojiSkin
import com.wallwar.data.EmojiSkinCatalog
import com.wallwar.data.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmojiShopViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val soundManager: SoundManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    val unlockedEmojiIds: StateFlow<Set<String>> = authRepository.unlockedEmojiIds

    val allEmojis: List<EmojiSkin> = EmojiSkinCatalog.ALL_EMOJIS

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _previewEmoji = MutableStateFlow<EmojiSkin?>(null)
    val previewEmoji: StateFlow<EmojiSkin?> = _previewEmoji.asStateFlow()

    fun preview(emoji: EmojiSkin) {
        _previewEmoji.value = emoji
        soundManager.playButtonClick()
        viewModelScope.launch {
            delay(3000)
            if (_previewEmoji.value?.id == emoji.id) {
                _previewEmoji.value = null
            }
        }
    }

    fun buyEmoji(emoji: EmojiSkin, onNavigateToCoinShop: () -> Unit) {
        val currentCoins = userProfile.value.coins
        if (currentCoins < emoji.priceCoins) {
            _statusMessage.value = "Need ${emoji.priceCoins - currentCoins} more coins to unlock ${emoji.symbol} ${emoji.name}!"
            soundManager.playInvalidMove()
            return
        }

        val success = authRepository.unlockEmojiSkin(emoji.id, emoji.priceCoins)
        if (success) {
            soundManager.playVictory()
            _previewEmoji.value = emoji
            _statusMessage.value = "🎉 Unlocked ${emoji.symbol} ${emoji.name}! It's permanently saved to your account."
            viewModelScope.launch {
                delay(3000)
                if (_previewEmoji.value?.id == emoji.id) {
                    _previewEmoji.value = null
                }
            }
        } else {
            _statusMessage.value = "Failed to unlock emoji. Insufficient coins."
            soundManager.playInvalidMove()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
