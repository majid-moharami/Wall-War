package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.SettingsRepository
import com.example.data.nakama.NakamaConfig
import com.example.data.nakama.NakamaRepository
import com.example.model.BoardTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository,
    private val nakamaRepository: NakamaRepository
) : ViewModel() {

    val boardTheme: StateFlow<BoardTheme> = settingsRepository.boardTheme
    val nakamaConfig: StateFlow<NakamaConfig> = nakamaRepository.config

    fun setBoardTheme(theme: BoardTheme) {
        settingsRepository.setBoardTheme(theme)
    }

    fun updateNakamaConfig(host: String, port: Int, serverKey: String, useSsl: Boolean) {
        nakamaRepository.updateConfig(host, port, serverKey, useSsl)
    }

    fun testNakamaConnection(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = nakamaRepository.authenticateWithDevice("TestDevice")
            onResult(success)
        }
    }
}
