package com.wallwar.data

import com.wallwar.data.nakama.NakamaRepository
import com.wallwar.model.BoardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val nakamaRepository: NakamaRepository
) {
    private val _boardTheme = MutableStateFlow(BoardTheme.ELEGANT_DARK)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    fun setBoardTheme(theme: BoardTheme, syncToServer: Boolean = true) {
        _boardTheme.value = theme
        if (syncToServer) {
            CoroutineScope(Dispatchers.IO).launch {
                nakamaRepository.syncUserSettingsToNakama(theme)
            }
        }
    }

    fun restoreDefaults() {
        setBoardTheme(BoardTheme.ELEGANT_DARK)
    }
}
