package com.wallwar.data

import android.content.Context
import android.content.SharedPreferences
import com.wallwar.data.nakama.NakamaRepository
import com.wallwar.model.AiDifficulty
import com.wallwar.model.OpponentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class DailyMission(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val target: Int,
    val currentProgress: Int,
    val coinReward: Int,
    val xpReward: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean
)

@Singleton
class DailyMissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nakamaRepository: NakamaRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wall_war_missions", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _missions = MutableStateFlow<List<DailyMission>>(loadDailyMissions())
    val missions: StateFlow<List<DailyMission>> = _missions.asStateFlow()

    companion object {
        data class MissionTemplate(
            val id: String,
            val title: String,
            val description: String,
            val icon: String,
            val target: Int,
            val coinReward: Int,
            val xpReward: Int
        )

        val MISSION_POOL = listOf(
            MissionTemplate(
                id = "win_3_matches",
                title = "Victory March",
                description = "Win 3 matches in any game mode",
                icon = "🏆",
                target = 3,
                coinReward = 75,
                xpReward = 100
            ),
            MissionTemplate(
                id = "place_15_walls",
                title = "Tactical Builder",
                description = "Place 15 walls across matches",
                icon = "🧱",
                target = 15,
                coinReward = 50,
                xpReward = 75
            ),
            MissionTemplate(
                id = "win_online_match",
                title = "Arena Conqueror",
                description = "Win 1 Online Multiplayer match",
                icon = "🌐",
                target = 1,
                coinReward = 100,
                xpReward = 150
            ),
            MissionTemplate(
                id = "speed_demon",
                title = "Speed Demon",
                description = "Win a match in under 60 seconds",
                icon = "⚡",
                target = 1,
                coinReward = 60,
                xpReward = 80
            ),
            MissionTemplate(
                id = "defeat_pro_ai",
                title = "Cyber Duelist",
                description = "Defeat Pro difficulty AI Bot",
                icon = "🤖",
                target = 1,
                coinReward = 80,
                xpReward = 120
            ),
            MissionTemplate(
                id = "wall_fortress",
                title = "Fortress Master",
                description = "Place 6 or more walls in a single match",
                icon = "🛡️",
                target = 1,
                coinReward = 55,
                xpReward = 70
            ),
            MissionTemplate(
                id = "win_streak_2",
                title = "Unstoppable Force",
                description = "Achieve a 2-game win streak",
                icon = "🔥",
                target = 2,
                coinReward = 90,
                xpReward = 125
            ),
            MissionTemplate(
                id = "play_5_matches",
                title = "Combat Veteran",
                description = "Complete 5 matches in any mode",
                icon = "⚔️",
                target = 5,
                coinReward = 70,
                xpReward = 100
            ),
            MissionTemplate(
                id = "minimalist_win",
                title = "Ghost Infiltrator",
                description = "Win a match using 2 or fewer walls",
                icon = "🎯",
                target = 1,
                coinReward = 65,
                xpReward = 90
            ),
            MissionTemplate(
                id = "fast_turn_win",
                title = "Blitz Strategist",
                description = "Win a match in 16 moves or fewer",
                icon = "⏱️",
                target = 1,
                coinReward = 60,
                xpReward = 85
            ),
            MissionTemplate(
                id = "high_stakes_arena",
                title = "High Roller",
                description = "Play in Neon Blitz or higher Arena",
                icon = "💎",
                target = 1,
                coinReward = 85,
                xpReward = 110
            )
        )
    }

    init {
        scope.launch {
            syncFromNakama()
        }
    }

    private fun todayDateUtc(): String {
        return LocalDate.now(ZoneOffset.UTC).toString()
    }

    fun getDailyMissions(): List<DailyMission> {
        _missions.value = loadDailyMissions()
        return _missions.value
    }

    private fun loadDailyMissions(): List<DailyMission> {
        val today = todayDateUtc()
        val storedDate = prefs.getString("missions_date", null)

        if (storedDate != today) {
            // Pick 3 pseudo-random missions deterministically for the day
            val dateEpochDay = try { LocalDate.parse(today).toEpochDay() } catch (_: Exception) { 0L }
            val rng = Random(dateEpochDay)
            val selectedIndices = MISSION_POOL.indices.shuffled(rng).take(3)
            val selectedTemplates = selectedIndices.map { MISSION_POOL[it] }

            val editor = prefs.edit()
            editor.putString("missions_date", today)
            editor.putInt("missions_count", selectedTemplates.size)

            selectedTemplates.forEachIndexed { index, t ->
                editor.putString("mission_${index}_id", t.id)
                editor.putInt("mission_${index}_progress", 0)
                editor.putBoolean("mission_${index}_claimed", false)
            }
            editor.apply()

            return selectedTemplates.map { t ->
                DailyMission(
                    id = t.id,
                    title = t.title,
                    description = t.description,
                    icon = t.icon,
                    target = t.target,
                    currentProgress = 0,
                    coinReward = t.coinReward,
                    xpReward = t.xpReward,
                    isCompleted = false,
                    isClaimed = false
                )
            }
        }

        val count = prefs.getInt("missions_count", 0)
        if (count == 0) {
            prefs.edit().remove("missions_date").apply()
            return loadDailyMissions()
        }

        val result = mutableListOf<DailyMission>()
        for (i in 0 until count) {
            val id = prefs.getString("mission_${i}_id", "") ?: ""
            val template = MISSION_POOL.find { it.id == id } ?: continue
            val progress = prefs.getInt("mission_${i}_progress", 0)
            val claimed = prefs.getBoolean("mission_${i}_claimed", false)
            val completed = progress >= template.target

            result.add(
                DailyMission(
                    id = template.id,
                    title = template.title,
                    description = template.description,
                    icon = template.icon,
                    target = template.target,
                    currentProgress = progress.coerceAtMost(template.target),
                    coinReward = template.coinReward,
                    xpReward = template.xpReward,
                    isCompleted = completed,
                    isClaimed = claimed
                )
            )
        }
        return result
    }

    suspend fun syncFromNakama() {
        try {
            val serverMissions = nakamaRepository.fetchDailyMissionsFromNakama()
            if (serverMissions != null) {
                val serverDate = serverMissions.optString("date", "")
                val today = todayDateUtc()
                if (serverDate == today) {
                    val array = serverMissions.optJSONArray("missions")
                    if (array != null && array.length() > 0) {
                        val editor = prefs.edit()
                        editor.putString("missions_date", today)
                        editor.putInt("missions_count", array.length())
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            editor.putString("mission_${i}_id", obj.getString("id"))
                            editor.putInt("mission_${i}_progress", obj.getInt("progress"))
                            editor.putBoolean("mission_${i}_claimed", obj.getBoolean("claimed"))
                        }
                        editor.apply()
                        _missions.value = loadDailyMissions()
                    }
                }
            }
        } catch (_: Exception) { }
    }

    fun recordMatchPlayed(
        didWin: Boolean,
        opponentType: OpponentType,
        aiDifficulty: AiDifficulty,
        wallsPlaced: Int,
        totalMoves: Int,
        durationSeconds: Long,
        arenaId: String,
        currentWinStreak: Int
    ) {
        val currentList = loadDailyMissions().toMutableList()
        val editor = prefs.edit()

        currentList.forEachIndexed { index, mission ->
            if (mission.isClaimed) return@forEachIndexed

            var progressIncrement = 0

            when (mission.id) {
                "win_3_matches" -> if (didWin) progressIncrement = 1
                "place_15_walls" -> progressIncrement = wallsPlaced
                "win_online_match" -> if (didWin && opponentType == OpponentType.ONLINE) progressIncrement = 1
                "speed_demon" -> if (didWin && durationSeconds in 1..60) progressIncrement = 1
                "defeat_pro_ai" -> if (didWin && opponentType == OpponentType.AI && aiDifficulty == AiDifficulty.PRO) progressIncrement = 1
                "wall_fortress" -> if (wallsPlaced >= 6) progressIncrement = 1
                "win_streak_2" -> if (currentWinStreak >= 2) progressIncrement = 2
                "play_5_matches" -> progressIncrement = 1
                "minimalist_win" -> if (didWin && wallsPlaced <= 2) progressIncrement = 1
                "fast_turn_win" -> if (didWin && totalMoves in 1..16) progressIncrement = 1
                "high_stakes_arena" -> if (arenaId != "training" && arenaId != "offline_ai") progressIncrement = 1
            }

            if (progressIncrement > 0) {
                val newProgress = (mission.currentProgress + progressIncrement).coerceAtMost(mission.target)
                editor.putInt("mission_${index}_progress", newProgress)
            }
        }
        editor.apply()
        _missions.value = loadDailyMissions()

        // Sync progress to cloud
        syncToNakama()
    }

    fun claimMissionReward(missionId: String): Pair<Int, Int>? {
        val currentList = loadDailyMissions()
        val missionIndex = currentList.indexOfFirst { it.id == missionId }
        if (missionIndex == -1) return null

        val mission = currentList[missionIndex]
        if (!mission.isCompleted || mission.isClaimed) return null

        prefs.edit().putBoolean("mission_${missionIndex}_claimed", true).apply()
        _missions.value = loadDailyMissions()

        scope.launch {
            try {
                nakamaRepository.rpcProcessCoinTransaction(mission.coinReward, "daily_mission_${mission.id}")
                syncToNakama()
            } catch (_: Exception) { }
        }

        return Pair(mission.coinReward, mission.xpReward)
    }

    private fun syncToNakama() {
        scope.launch {
            try {
                val list = _missions.value
                val array = JSONArray()
                list.forEach { m ->
                    val obj = JSONObject().apply {
                        put("id", m.id)
                        put("progress", m.currentProgress)
                        put("claimed", m.isClaimed)
                    }
                    array.put(obj)
                }
                nakamaRepository.syncDailyMissionsToNakama(todayDateUtc(), array)
            } catch (_: Exception) { }
        }
    }
}
