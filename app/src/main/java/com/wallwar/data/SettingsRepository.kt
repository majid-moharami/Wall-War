package com.wallwar.data

import android.content.Context
import android.content.SharedPreferences
import com.wallwar.data.nakama.NakamaRepository
import com.wallwar.model.BoardTheme
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val nakamaRepository: NakamaRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_settings_prefs", Context.MODE_PRIVATE)

    private val _boardTheme = MutableStateFlow(loadStoredTheme())
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    private fun loadStoredTheme(): BoardTheme {
        val themeName = prefs.getString("selected_board_theme", BoardTheme.ELEGANT_DARK.name) ?: BoardTheme.ELEGANT_DARK.name
        return try {
            BoardTheme.valueOf(themeName)
        } catch (e: Exception) {
            BoardTheme.ELEGANT_DARK
        }
    }

    fun setBoardTheme(theme: BoardTheme, syncToServer: Boolean = true) {
        _boardTheme.value = theme
        prefs.edit().putString("selected_board_theme", theme.name).apply()
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
