package com.wallwar.data

import androidx.compose.ui.graphics.Color

data class Arena(
    val id: String,
    val title: String,
    val subtitle: String,
    val entryFee: Int,
    val winningPrize: Int,
    val colorHex: Long,
    val minCoinsRequired: Int = entryFee,
    val isPopular: Boolean = false,
    val isBestValue: Boolean = false,
    val isOffline: Boolean = false
) {
    val houseFee: Int get() = if (winningPrize > 0) (entryFee * 2) - winningPrize else 0
}

object ArenaConfig {
    // 5 Online Multi-player Arena Tiers
    val onlineArenas = listOf(
        Arena(
            id = "novice",
            title = "Novice Table",
            subtitle = "Low-risk starter table for entry duelists",
            entryFee = 10,
            winningPrize = 18,
            colorHex = 0xFF22C55E // Emerald Green
        ),
        Arena(
            id = "amateur",
            title = "Amateur Arena",
            subtitle = "Balanced stakes for casual tacticians",
            entryFee = 25,
            winningPrize = 45,
            colorHex = 0xFF3B82F6 // Royal Blue
        ),
        Arena(
            id = "pro",
            title = "Pro Arena",
            subtitle = "Competitive arena for strategy masters",
            entryFee = 100,
            winningPrize = 180,
            colorHex = 0xFF00E5FF, // Neon Cyan
            isPopular = true
        ),
        Arena(
            id = "highroller",
            title = "High Roller",
            subtitle = "High-stakes table for confident wagerers",
            entryFee = 250,
            winningPrize = 450,
            colorHex = 0xFFE056FD // Neon Magenta
        ),
        Arena(
            id = "master",
            title = "Master Duel",
            subtitle = "Apex table for grandmaster cyber-warriors",
            entryFee = 1000,
            winningPrize = 1800,
            colorHex = 0xFFFFD700, // Gold / Amber
            isBestValue = true
        )
    )

    // Offline / AI Battle Arena Definition (0 Coins Prize to prevent farming)
    val offlineAiArena = Arena(
        id = "offline_ai",
        title = "AI / Practice Battle",
        subtitle = "Practice tactics offline or against AI (0 Coins Reward)",
        entryFee = 50,
        winningPrize = 0,
        colorHex = 0xFF0EA5E9, // Electric Cyan Blue
        isOffline = true
    )

    val defaultArenas = onlineArenas

    fun getArenaById(id: String): Arena {
        if (id == "offline_ai") return offlineAiArena
        return onlineArenas.find { it.id == id } ?: onlineArenas[0]
    }
}
