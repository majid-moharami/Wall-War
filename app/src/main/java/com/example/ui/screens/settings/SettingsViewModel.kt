package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.example.audio.SoundManager
import com.example.data.SettingsRepository
import com.example.model.BoardTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val boardTheme: StateFlow<BoardTheme> = settingsRepository.boardTheme

    fun setBoardTheme(theme: BoardTheme) {
        settingsRepository.setBoardTheme(theme)
    }
}
