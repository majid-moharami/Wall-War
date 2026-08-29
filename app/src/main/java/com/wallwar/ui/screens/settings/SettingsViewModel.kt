package com.wallwar.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.audio.SoundManager
import com.wallwar.data.AuthRepository
import com.wallwar.data.GameRepository
import com.wallwar.data.MatchRecord
import com.wallwar.data.SettingsRepository
import com.wallwar.data.UserProfile
import com.wallwar.data.nakama.NakamaConfig
import com.wallwar.data.nakama.NakamaRepository
import com.wallwar.model.BoardTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository,
    private val nakamaRepository: NakamaRepository,
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    val boardTheme: StateFlow<BoardTheme> = settingsRepository.boardTheme
    val nakamaConfig: StateFlow<NakamaConfig> = nakamaRepository.config
    val userProfile: StateFlow<UserProfile> = authRepository.userProfile

    fun updateCoinsAndLevel(targetCoins: Int, targetLevel: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.setUserDevLevelAndCoins(targetLevel, targetCoins)
                onResult(true, "Applied! Level: $targetLevel, Coins: $targetCoins (Synced to server).")
            } catch (e: Exception) {
                onResult(false, "Server sync error: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun boostLevelAndCoins(targetLevel: Int = 30, targetCoins: Int = 2000000, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.setUserDevLevelAndCoins(targetLevel, targetCoins)
                onResult(true, "Boost applied! Level set to $targetLevel, Coins set to 2,000,000 & synced to Server.")
            } catch (e: Exception) {
                onResult(false, "Server sync error: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun setBoardTheme(theme: BoardTheme) {
        settingsRepository.setBoardTheme(theme)
    }

    fun updateNakamaConfig(host: String, port: Int, serverKey: String, useSsl: Boolean) {
        nakamaRepository.updateConfig(host, port, serverKey, useSsl)
    }

    fun testNakamaConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = nakamaRepository.testConnectionDetailed("TestDevice")
            onResult(result.first, result.second)
        }
    }

    fun restoreFromNakamaServer(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authRepository.syncFromNakamaServer()
                onResult(true, "Profile, coins, trophies, and theme successfully restored from Nakama Cloud!")
            } catch (e: Exception) {
                onResult(false, "Server restore failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun exportDataBackup(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = authRepository.userProfile.value
                val theme = settingsRepository.boardTheme.value.name
                val config = nakamaRepository.config.value
                val matches = gameRepository.allMatches.firstOrNull() ?: emptyList()

                val json = JSONObject()

                val profileObj = JSONObject().apply {
                    put("displayName", user.displayName)
                    put("email", user.email)
                    put("trophies", user.trophies)
                    put("xp", user.xp)
                    put("level", user.level)
                    put("rankTitle", user.rankTitle)
                    put("wins", user.wins)
                    put("totalMatches", user.totalMatches)
                    put("wallsPlaced", user.wallsPlaced)
                    put("coins", user.coins)
                }
                json.put("profile", profileObj)

                val settingsObj = JSONObject().apply {
                    put("boardTheme", theme)
                    put("soundEnabled", soundManager.isSoundEnabled)
                    put("vibrationEnabled", soundManager.isVibrationEnabled)
                    put("nakamaHost", config.host)
                    put("nakamaPort", config.port)
                    put("nakamaKey", config.serverKey)
                }
                json.put("settings", settingsObj)

                val matchesArr = JSONArray()
                matches.forEach { match ->
                    val matchObj = JSONObject().apply {
                        put("modeName", match.modeName)
                        put("opponentName", match.opponentName)
                        put("winnerPlayer", match.winnerPlayer)
                        put("totalMoves", match.totalMoves)
                        put("totalWallsPlaced", match.totalWallsPlaced)
                        put("durationSeconds", match.durationSeconds)
                        put("timestamp", match.timestamp)
                    }
                    matchesArr.put(matchObj)
                }
                json.put("matches", matchesArr)
                json.put("version", "1.0")

                onResult(json.toString(2))
            } catch (e: Exception) {
                onResult("")
            }
        }
    }

    fun restoreDataFromBackup(jsonStr: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject(jsonStr)

                if (json.has("profile")) {
                    val p = json.getJSONObject("profile")
                    val restoredEmail = p.optString("email", "").takeIf { it.isNotBlank() && it != "guest@wallwar.app" }
                    val restoredProfile = UserProfile(
                        isLoggedIn = true,
                        displayName = p.optString("displayName", "Restored Duelist"),
                        email = restoredEmail,
                        trophies = p.optInt("trophies", 0),
                        xp = p.optInt("xp", 0),
                        level = p.optInt("level", 1),
                        rankTitle = p.optString("rankTitle", "Novice Duelist"),
                        wins = p.optInt("wins", 0),
                        totalMatches = p.optInt("totalMatches", 0),
                        wallsPlaced = p.optInt("wallsPlaced", 0),
                        coins = p.optInt("coins", 150)
                    )
                    authRepository.restoreProfileData(restoredProfile)
                }

                if (json.has("settings")) {
                    val s = json.getJSONObject("settings")
                    val themeStr = s.optString("boardTheme", "ELEGANT_DARK")
                    try {
                        val themeEnum = BoardTheme.valueOf(themeStr)
                        settingsRepository.setBoardTheme(themeEnum)
                    } catch (_: Exception) {}

                    soundManager.isSoundEnabled = s.optBoolean("soundEnabled", true)
                    soundManager.isVibrationEnabled = s.optBoolean("vibrationEnabled", true)

                    val host = s.optString("nakamaHost", "")
                    val port = s.optInt("nakamaPort", 7350)
                    val key = s.optString("nakamaKey", "defaultkey")
                    if (host.isNotBlank()) {
                        nakamaRepository.updateConfig(host, port, key, false)
                    }
                }

                var restoredMatchCount = 0
                if (json.has("matches")) {
                    val mArr = json.getJSONArray("matches")
                    val matchList = mutableListOf<MatchRecord>()
                    for (i in 0 until mArr.length()) {
                        val m = mArr.getJSONObject(i)
                        matchList.add(
                            MatchRecord(
                                modeName = m.optString("modeName", "Tactical Match"),
                                opponentName = m.optString("opponentName", "Opponent"),
                                winnerPlayer = m.optInt("winnerPlayer", 0),
                                totalMoves = m.optInt("totalMoves", 10),
                                totalWallsPlaced = m.optInt("totalWallsPlaced", 0),
                                durationSeconds = m.optLong("durationSeconds", 60L),
                                timestamp = m.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    if (matchList.isNotEmpty()) {
                        gameRepository.restoreMatches(matchList)
                        restoredMatchCount = matchList.size
                    }
                }

                onResult(true, "Successfully restored backup data! ($restoredMatchCount match logs imported)")
            } catch (e: Exception) {
                onResult(false, "Failed to restore backup: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun restoreDefaultSettings() {
        soundManager.isSoundEnabled = true
        soundManager.isVibrationEnabled = true
        settingsRepository.restoreDefaults()
        nakamaRepository.updateConfig("https://nakama.wallwargame.com", 7350, "defaultkey", true)
    }
}
