package com.wallwar.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object RankingRoute

@Serializable
object ProfileRoute

@Serializable
data class GameBoardRoute(
    val mode: String = "DUEL",
    val opponent: String = "AI",
    val difficulty: String = "NORMAL"
)

@Serializable
object RulesRoute

@Serializable
object HistoryRoute

@Serializable
object SettingsRoute
