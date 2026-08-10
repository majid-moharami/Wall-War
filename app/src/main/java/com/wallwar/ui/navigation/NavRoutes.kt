package com.wallwar.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object AuthRoute

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
    val difficulty: String = "NORMAL",
    val arenaId: String = "pro"
)

@Serializable
object RulesRoute

@Serializable
object HistoryRoute

@Serializable
object SettingsRoute

@Serializable
object CoinShopRoute
