package com.wallwar.data

data class EmojiSkin(
    val id: String,
    val symbol: String,
    val name: String,
    val description: String,
    val priceCoins: Int,
    val isDefaultUnlocked: Boolean = false,
    val tag: String = "Greedy & Taunt"
) {
    val coinPrice: Int get() = priceCoins
}

object EmojiSkinCatalog {
    val ALL_EMOJIS: List<EmojiSkin> = listOf(
        // 1. Unlocked by default
        EmojiSkin(
            id = "emoji_cool",
            symbol = "😎",
            name = "Smug Duelist",
            description = "Too cool for your walls. Smooth victory vibes.",
            priceCoins = 0,
            isDefaultUnlocked = true,
            tag = "Default Unlocked"
        ),
        // 2. Unlocked by default
        EmojiSkin(
            id = "emoji_smirk",
            symbol = "😈",
            name = "Trap Master",
            description = "Enjoying the wall trap you just set up.",
            priceCoins = 0,
            isDefaultUnlocked = true,
            tag = "Default Unlocked"
        ),
        // 3. Paid
        EmojiSkin(
            id = "emoji_greedy",
            symbol = "🤑",
            name = "Coin Greedy",
            description = "Here strictly for your coins and trophies.",
            priceCoins = 350,
            isDefaultUnlocked = false,
            tag = "Greedy"
        ),
        // 4. Paid
        EmojiSkin(
            id = "emoji_fire",
            symbol = "🔥",
            name = "On Fire",
            description = "Unstoppable momentum. Burning through the maze.",
            priceCoins = 500,
            isDefaultUnlocked = false,
            tag = "Hot Streak"
        ),
        // 5. Paid
        EmojiSkin(
            id = "emoji_mindblown",
            symbol = "🤯",
            name = "Big Brain",
            description = "Calculated 10 moves ahead. Outsmarted!",
            priceCoins = 750,
            isDefaultUnlocked = false,
            tag = "Tactical"
        ),
        // 6. Paid
        EmojiSkin(
            id = "emoji_laugh",
            symbol = "🤣",
            name = "Gotcha!",
            description = "Laughing as your opponent walks into a dead end.",
            priceCoins = 1000,
            isDefaultUnlocked = false,
            tag = "Taunt"
        ),
        // 7. Paid
        EmojiSkin(
            id = "emoji_brick",
            symbol = "🧱",
            name = "Brick Wall",
            description = "Impenetrable defense. You shall not pass!",
            priceCoins = 1250,
            isDefaultUnlocked = false,
            tag = "Defense"
        ),
        // 8. Paid
        EmojiSkin(
            id = "emoji_crown",
            symbol = "👑",
            name = "Arena King",
            description = "Bow down to the true master of Wall War.",
            priceCoins = 1750,
            isDefaultUnlocked = false,
            tag = "Legendary"
        ),
        // 9. Paid
        EmojiSkin(
            id = "emoji_skull",
            symbol = "💀",
            name = "Game Over",
            description = "Rest in peace, opponent pawn. It's finished.",
            priceCoins = 2500,
            isDefaultUnlocked = false,
            tag = "Fatal Taunt"
        )
    )

    val DEFAULT_UNLOCKED_IDS: Set<String> = ALL_EMOJIS
        .filter { it.isDefaultUnlocked }
        .map { it.id }
        .toSet()

    fun getEmojiById(id: String): EmojiSkin? = ALL_EMOJIS.find { it.id == id }
    
    fun getById(id: String): EmojiSkin? = getEmojiById(id)
    
    fun getSymbol(id: String): String = getEmojiById(id)?.symbol ?: "😎"
}
