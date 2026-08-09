package com.wallwar.data

import androidx.compose.ui.graphics.Color
import com.wallwar.model.BoardTheme

data class Arena(
    val id: String,
    val title: String,
    val subtitle: String,
    val entryFee: Int,
    val winningPrize: Int,
    val colorHex: Long,
    val boardTheme: BoardTheme,
    val minCoinsRequired: Int = entryFee,
    val isPopular: Boolean = false,
    val isBestValue: Boolean = false,
    val isOffline: Boolean = false
) {
    val houseFee: Int get() = if (winningPrize > 0) (entryFee * 2) - winningPrize else 0
}

object ArenaConfig {
    // 7 Online Multi-player Arena Tiers with scaling Board Themes & Entry Fees
    val onlineArenas = listOf(
        Arena(
            id = "starter",
            title = "Starter Table",
            subtitle = "Low-risk starter duel with classic neon styling",
            entryFee = 25,
            winningPrize = 45,
            colorHex = 0xFF3B82F6, // Classic Blue
            boardTheme = BoardTheme.STARTER
        ),
        Arena(
            id = "novice",
            title = "Novice Arena",
            subtitle = "Clean subtle metallic grid for aspiring tacticians",
            entryFee = 50,
            winningPrize = 90,
            colorHex = 0xFF00E5FF, // Cyan / Orange
            boardTheme = BoardTheme.NOVICE
        ),
        Arena(
            id = "amateur",
            title = "Amateur Club",
            subtitle = "Carbon fiber battlefield for competitive duelists",
            entryFee = 100,
            winningPrize = 180,
            colorHex = 0xFFE056FD, // Hot Pink & Electric Violet
            boardTheme = BoardTheme.AMATEUR
        ),
        Arena(
            id = "pro",
            title = "Pro Arena",
            subtitle = "High-tech matrix circuit tiles for strategy masters",
            entryFee = 250,
            winningPrize = 450,
            colorHex = 0xFF00FF87, // Emerald & Neon Mint
            boardTheme = BoardTheme.PRO,
            isPopular = true
        ),
        Arena(
            id = "highroller",
            title = "High Roller",
            subtitle = "Volcanic rock arena with glowing molten seams",
            entryFee = 500,
            winningPrize = 900,
            colorHex = 0xFFFF5500, // Amber Flame & Crimson
            boardTheme = BoardTheme.HIGH_ROLLER
        ),
        Arena(
            id = "master",
            title = "Master Duel",
            subtitle = "Apex dark crystal tiles with pulsing purple grid lines",
            entryFee = 1000,
            winningPrize = 1800,
            colorHex = 0xFF9D4EDD, // Deep Indigo & Plasma Violet
            boardTheme = BoardTheme.MASTER
        ),
        Arena(
            id = "grandchampion",
            title = "Grand Champion",
            subtitle = "Ultimate Royal Gold & Cosmic White glow with obsidian tiles",
            entryFee = 5000,
            winningPrize = 9000,
            colorHex = 0xFFFFD700, // Royal Gold
            boardTheme = BoardTheme.GRAND_CHAMPION,
            isBestValue = true
        )
    )

    // Offline / AI Battle Arena Definition
    val offlineAiArena = Arena(
        id = "offline_ai",
        title = "AI / Practice Battle",
        subtitle = "Practice tactics offline or against AI (0 Coins Reward)",
        entryFee = 50,
        winningPrize = 0,
        colorHex = 0xFF0EA5E9, // Electric Cyan Blue
        boardTheme = BoardTheme.STARTER,
        isOffline = true
    )

    val defaultArenas = onlineArenas

    fun getArenaById(id: String): Arena {
        if (id == "offline_ai") return offlineAiArena
        return onlineArenas.find { it.id == id } ?: onlineArenas[0]
    }
}

