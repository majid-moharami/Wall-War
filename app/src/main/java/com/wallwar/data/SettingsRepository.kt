package com.wallwar.data

import com.wallwar.model.BoardTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor() {
    private val _boardTheme = MutableStateFlow(BoardTheme.ELEGANT_DARK)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    fun setBoardTheme(theme: BoardTheme) {
        _boardTheme.value = theme
    }
}
