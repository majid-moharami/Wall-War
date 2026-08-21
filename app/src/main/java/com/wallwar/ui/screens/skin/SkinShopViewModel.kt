package com.wallwar.ui.screens.skin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.BallSkin
import com.wallwar.data.BallSkinCatalog
import com.wallwar.data.EmojiSkin
import com.wallwar.data.EmojiSkinCatalog
import com.wallwar.data.ProfileSkin
import com.wallwar.data.ProfileSkinCatalog
import com.wallwar.data.UserProfile
import com.wallwar.data.WallSkin
import com.wallwar.data.WallSkinCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkinShopViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    val soundManager: SoundManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfile
    val unlockedBallSkinIds: StateFlow<Set<String>> = authRepository.unlockedBallSkinIds
    val equippedBallSkinId: StateFlow<String> = authRepository.equippedBallSkinId
    val unlockedWallSkinIds: StateFlow<Set<String>> = authRepository.unlockedWallSkinIds
    val equippedWallSkinId: StateFlow<String> = authRepository.equippedWallSkinId
    val unlockedEmojiIds: StateFlow<Set<String>> = authRepository.unlockedEmojiIds
    val unlockedAvatarSkinIds: StateFlow<Set<String>> = authRepository.unlockedAvatarSkinIds

    val allBallSkins: List<BallSkin> = BallSkinCatalog.ALL_BALL_SKINS
    val allWallSkins: List<WallSkin> = WallSkinCatalog.ALL_WALL_SKINS
    val allEmojis: List<EmojiSkin> = EmojiSkinCatalog.ALL_EMOJIS
    val allProfileSkins: List<ProfileSkin> = ProfileSkinCatalog.ALL_SKINS

    private val initialTab = savedStateHandle.get<Int>("initialTab") ?: 0
    private val _selectedTab = MutableStateFlow(initialTab.coerceIn(0, 3))
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _previewBallSkin = MutableStateFlow<BallSkin?>(null)
    val previewBallSkin: StateFlow<BallSkin?> = _previewBallSkin.asStateFlow()

    private val _previewWallSkin = MutableStateFlow<WallSkin?>(null)
    val previewWallSkin: StateFlow<WallSkin?> = _previewWallSkin.asStateFlow()

    private val _previewEmoji = MutableStateFlow<EmojiSkin?>(null)
    val previewEmoji: StateFlow<EmojiSkin?> = _previewEmoji.asStateFlow()

    private val _previewProfileSkin = MutableStateFlow<ProfileSkin?>(null)
    val previewProfileSkin: StateFlow<ProfileSkin?> = _previewProfileSkin.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    data class InsufficientCoinsInfo(
        val itemName: String,
        val price: Int,
        val shortage: Int
    )

    private val _insufficientCoinsInfo = MutableStateFlow<InsufficientCoinsInfo?>(null)
    val insufficientCoinsInfo: StateFlow<InsufficientCoinsInfo?> = _insufficientCoinsInfo.asStateFlow()

    fun dismissInsufficientCoinsDialog() {
        _insufficientCoinsInfo.value = null
    }

    fun selectTab(index: Int) {
        soundManager.playButtonClick()
        _selectedTab.value = index.coerceIn(0, 3)
    }

    fun previewBall(skin: BallSkin) {
        soundManager.playButtonClick()
        _previewBallSkin.value = skin
    }

    fun clearBallPreview() {
        _previewBallSkin.value = null
    }

    fun previewWall(skin: WallSkin) {
        soundManager.playButtonClick()
        _previewWallSkin.value = skin
    }

    fun clearWallPreview() {
        _previewWallSkin.value = null
    }

    fun buyWall(skin: WallSkin) {
        if (skin.isFree || unlockedWallSkinIds.value.contains(skin.id)) {
            equipWall(skin)
            return
        }

        val currentLevel = userProfile.value.level
        if (currentLevel < skin.requiredLevel) {
            soundManager.playErrorSound()
            _statusMessage.value = "🔒 Level ${skin.requiredLevel} Required! (You are Level $currentLevel)"
            return
        }

        val currentCoins = userProfile.value.coins
        if (currentCoins < skin.priceCoins) {
            soundManager.playErrorSound()
            _insufficientCoinsInfo.value = InsufficientCoinsInfo(
                itemName = skin.name,
                price = skin.priceCoins,
                shortage = skin.priceCoins - currentCoins
            )
            return
        }

        val success = authRepository.unlockWallSkin(skin.id, skin.priceCoins)
        if (success) {
            authRepository.equipWallSkin(skin.id)
            soundManager.playRewardSound()
            _statusMessage.value = "Unlocked & Equipped '${skin.name}'! 🧱"
        } else {
            soundManager.playErrorSound()
            _insufficientCoinsInfo.value = InsufficientCoinsInfo(
                itemName = skin.name,
                price = skin.priceCoins,
                shortage = (skin.priceCoins - userProfile.value.coins).coerceAtLeast(0)
            )
        }
    }

    fun equipWall(skin: WallSkin) {
        authRepository.equipWallSkin(skin.id)
        soundManager.playButtonClick()
        _statusMessage.value = "Equipped '${skin.name}' wall skin! 🧱"
    }

    fun buyBall(skin: BallSkin) {
        if (skin.isFree || unlockedBallSkinIds.value.contains(skin.id)) {
            equipBall(skin)
            return
        }

        val currentLevel = userProfile.value.level
        if (currentLevel < skin.requiredLevel) {
            soundManager.playErrorSound()
            _statusMessage.value = "🔒 Level ${skin.requiredLevel} Required! (You are Level $currentLevel)"
            return
        }

        val currentCoins = userProfile.value.coins
        if (currentCoins < skin.priceCoins) {
            soundManager.playErrorSound()
            _insufficientCoinsInfo.value = InsufficientCoinsInfo(
                itemName = skin.name,
                price = skin.priceCoins,
                shortage = skin.priceCoins - currentCoins
            )
            return
        }

        val success = authRepository.unlockBallSkin(skin.id, skin.priceCoins)
        if (success) {
            authRepository.equipBallSkin(skin.id)
            soundManager.playRewardSound()
            _statusMessage.value = "Unlocked & Equipped '${skin.name}'! 🌟"
        } else {
            soundManager.playErrorSound()
            _insufficientCoinsInfo.value = InsufficientCoinsInfo(
                itemName = skin.name,
                price = skin.priceCoins,
                shortage = (skin.priceCoins - userProfile.value.coins).coerceAtLeast(0)
            )
        }
    }

    fun equipBall(skin: BallSkin) {
        authRepository.equipBallSkin(skin.id)
        soundManager.playButtonClick()
        _statusMessage.value = "Equipped '${skin.name}' ball skin! ⚽"
    }

    fun previewEmoji(emoji: EmojiSkin) {
        soundManager.playEmoteSound(emoji.id)
        _previewEmoji.value = emoji
    }

    fun clearEmojiPreview() {
        _previewEmoji.value = null
    }

    fun buyEmoji(emoji: EmojiSkin) {
        if (emoji.isDefaultUnlocked || unlockedEmojiIds.value.contains(emoji.id)) {
            _statusMessage.value = "You already own '${emoji.name}'!"
            return
        }

        val currentCoins = userProfile.value.coins
        if (currentCoins < emoji.priceCoins) {
            soundManager.playErrorSound()
            _insufficientCoinsInfo.value = InsufficientCoinsInfo(
                itemName = emoji.name,
                price = emoji.priceCoins,
                shortage = emoji.priceCoins - currentCoins
            )
            return
        }

        val success = authRepository.unlockEmojiSkin(emoji.id, emoji.priceCoins)
        if (success) {
            soundManager.playRewardSound()
            _statusMessage.value = "Unlocked '${emoji.name}' ${emoji.symbol}! 🥳"
        } else {
            soundManager.playErrorSound()
            _insufficientCoinsInfo.value = InsufficientCoinsInfo(
                itemName = emoji.name,
                price = emoji.priceCoins,
                shortage = (emoji.priceCoins - userProfile.value.coins).coerceAtLeast(0)
            )
        }
    }

    fun previewProfileSkin(skin: ProfileSkin) {
        soundManager.playButtonClick()
        _previewProfileSkin.value = skin
    }

    fun clearProfileSkinPreview() {
        _previewProfileSkin.value = null
    }

    fun buyProfileSkin(skin: ProfileSkin) {
        if (skin.isDefault || unlockedAvatarSkinIds.value.contains(skin.id)) {
            equipProfileSkin(skin)
            return
        }

        val currentCoins = userProfile.value.coins
        if (currentCoins < skin.priceCoins) {
            soundManager.playErrorSound()
            _insufficientCoinsInfo.value = InsufficientCoinsInfo(
                itemName = skin.name,
                price = skin.priceCoins,
                shortage = skin.priceCoins - currentCoins
            )
            return
        }

        val success = authRepository.unlockAvatarSkin(skin.id, skin.priceCoins)
        if (success) {
            authRepository.equipAvatarSkin(skin.id)
            soundManager.playRewardSound()
            _statusMessage.value = "Unlocked & Equipped '${skin.name}'! 🛡️"
        } else {
            soundManager.playErrorSound()
            _insufficientCoinsInfo.value = InsufficientCoinsInfo(
                itemName = skin.name,
                price = skin.priceCoins,
                shortage = (skin.priceCoins - userProfile.value.coins).coerceAtLeast(0)
            )
        }
    }

    fun equipProfileSkin(skin: ProfileSkin) {
        authRepository.equipAvatarSkin(skin.id)
        soundManager.playButtonClick()
        _statusMessage.value = "Equipped '${skin.name}' avatar! 👤"
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
