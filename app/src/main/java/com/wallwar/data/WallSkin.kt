package com.wallwar.data

import androidx.annotation.DrawableRes
import com.wallwar.R

data class WallSkin(
    val id: String,
    val name: String,
    @DrawableRes val drawableResId: Int,
    val priceCoins: Int,
    val description: String,
    val rarity: String,
    val tag: String,
    val isDefaultUnlocked: Boolean = false,
    val requiredLevel: Int = 1
) {
    val isFree: Boolean get() = priceCoins == 0 || isDefaultUnlocked
}

object WallSkinCatalog {
    // 0. Default Free Walls (Level 1)
    val DEFAULT_BLUE = WallSkin(
        id = "wall_blue",
        name = "Cyber Blue Wall",
        drawableResId = R.drawable.ic_blue_wall,
        priceCoins = 0,
        description = "Standard issue cyber blue reinforced energy barrier.",
        rarity = "Starter",
        tag = "Free Default",
        isDefaultUnlocked = true,
        requiredLevel = 1
    )

    val DEFAULT_RED = WallSkin(
        id = "wall_red",
        name = "Crimson Red Wall",
        drawableResId = R.drawable.ic_red_wall,
        priceCoins = 0,
        description = "Aggressive crimson tactical combat barricade.",
        rarity = "Starter",
        tag = "Free Default",
        isDefaultUnlocked = true,
        requiredLevel = 1
    )

    // 12 Paid Wall Skins sorted by level and price
    val ALL_WALL_SKINS: List<WallSkin> = listOf(
        DEFAULT_BLUE,
        DEFAULT_RED,
        // 1) 2,000 coins - Level 4
        WallSkin(
            id = "wall_wood",
            name = "Wood Wall",
            drawableResId = R.drawable.ic_wood_wall,
            priceCoins = 2000,
            description = "Solid timber hardwood palisade seasoned for tactical grid battles.",
            rarity = "Common",
            tag = "Rustic",
            requiredLevel = 4
        ),
        // 2) 3,000 coins - Level 5
        WallSkin(
            id = "wall_brick",
            name = "Brick Wall",
            drawableResId = R.drawable.ic_brick_wall,
            priceCoins = 3000,
            description = "Sturdy clay masonry wall offering unyielding tactical perimeter defense.",
            rarity = "Common",
            tag = "Masonry",
            requiredLevel = 5
        ),
        // 3) 3,500 coins - Level 6
        WallSkin(
            id = "wall_concrete",
            name = "Concrete Wall",
            drawableResId = R.drawable.ic_concrete_wall,
            priceCoins = 3500,
            description = "Reinforced industrial concrete slab capable of repelling heavy assaults.",
            rarity = "Common",
            tag = "Industrial",
            requiredLevel = 6
        ),
        // 4) 4,500 coins - Level 8
        WallSkin(
            id = "wall_laser",
            name = "Laser Wall",
            drawableResId = R.drawable.ic_laser_wall,
            priceCoins = 4500,
            description = "High-energy plasma laser grid cutting off opponent paths with focused light.",
            rarity = "Uncommon",
            tag = "High-Tech",
            requiredLevel = 8
        ),
        // 5) 7,000 coins - Level 10
        WallSkin(
            id = "wall_ice",
            name = "Ice Wall",
            drawableResId = R.drawable.ic_ice_wall,
            priceCoins = 7000,
            description = "Sub-zero crystalline glacial barricade freezing opponent momentum.",
            rarity = "Uncommon",
            tag = "Elemental",
            requiredLevel = 10
        ),
        // 6) 9,000 coins - Level 12
        WallSkin(
            id = "wall_cyber_shield",
            name = "Cyber Shield Wall",
            drawableResId = R.drawable.ic_cyber_shield_wall,
            priceCoins = 9000,
            description = "Futuristic hard-light energy forcefield with animated matrix shielding.",
            rarity = "Rare",
            tag = "Cybernetic",
            requiredLevel = 12
        ),
        // 7) 12,000 coins - Level 14
        WallSkin(
            id = "wall_ancient",
            name = "Ancient Wall",
            drawableResId = R.drawable.ic_anceint_wall,
            priceCoins = 12000,
            description = "Mystical runic stone monument imbued with centuries of defensive magic.",
            rarity = "Rare",
            tag = "Runic Relic",
            requiredLevel = 14
        ),
        // 8) 15,000 coins - Level 15
        WallSkin(
            id = "wall_gold",
            name = "Gold Wall",
            drawableResId = R.drawable.ic_gold_wall,
            priceCoins = 15000,
            description = "Opulent solid gold vault barrier adorned with champion battle engravings.",
            rarity = "Epic",
            tag = "Prestige",
            requiredLevel = 15
        ),
        // 9) 20,000 coins - Level 17
        WallSkin(
            id = "wall_holographic",
            name = "Holographic Wall",
            drawableResId = R.drawable.ic_holographic_wall,
            priceCoins = 20000,
            description = "Iridescent chromatic hologram refracting cyber wavelengths across the arena.",
            rarity = "Epic",
            tag = "Holo Grid",
            requiredLevel = 17
        ),
        // 10) 25,000 coins - Level 18
        WallSkin(
            id = "wall_dark_energy",
            name = "Dark Energy Wall",
            drawableResId = R.drawable.ic_dark_energy_wall,
            priceCoins = 25000,
            description = "Swirling anti-matter gravitational anomaly bending arena space-time.",
            rarity = "Legendary",
            tag = "Dark Matter",
            requiredLevel = 18
        ),
        // 11) 40,000 coins - Level 19
        WallSkin(
            id = "wall_diamond",
            name = "Diamond Wall",
            drawableResId = R.drawable.ic_diamond_wall,
            priceCoins = 40000,
            description = "Indestructible prismatic diamond gemstone facet shining with unmatched hardness.",
            rarity = "Mythic",
            tag = "Diamond",
            requiredLevel = 19
        ),
        // 12) 50,000 coins - Level 20
        WallSkin(
            id = "wall_eternity_void",
            name = "Eternity Void Wall",
            drawableResId = R.drawable.ic_eternity_void_wall,
            priceCoins = 50000,
            description = "Infinite astral abyss consuming light and matter at the cosmic edge of reality.",
            rarity = "Exalted",
            tag = "Celestial",
            requiredLevel = 20
        )
    ).sortedWith(compareBy<WallSkin> { it.requiredLevel }.thenBy { it.priceCoins })

    val DEFAULT_UNLOCKED_WALL_IDS: Set<String> = setOf("wall_blue", "wall_red")
    const val DEFAULT_EQUIPPED_WALL_ID = "wall_blue"
    const val DEFAULT_OPPONENT_WALL_ID = "wall_red"

    fun normalize(id: String): String {
        return id.lowercase().trim()
            .removePrefix("wall_")
            .removePrefix("ic_")
            .removeSuffix("_wall")
    }

    fun isFreeWallSkin(id: String?): Boolean {
        if (id.isNullOrBlank()) return true
        val norm = normalize(id)
        return norm == "blue" || norm == "red"
    }

    /**
     * Resolves wall skins for Player 0 and Player 1.
     * If both enter with identical default free walls, alternate one so they don't look identical.
     */
    fun resolveMatchWallSkins(
        p0SkinId: String?,
        p1SkinId: String?,
        userPlayerIndex: Int = 0
    ): Pair<String, String> {
        val s0 = if (p0SkinId.isNullOrBlank()) DEFAULT_EQUIPPED_WALL_ID else p0SkinId
        val s1 = if (p1SkinId.isNullOrBlank()) DEFAULT_OPPONENT_WALL_ID else p1SkinId

        val norm0 = normalize(s0)
        val norm1 = normalize(s1)

        if (norm0 == norm1 && (norm0 == "blue" || norm0 == "red")) {
            val alternateSkin = if (norm0 == "blue") DEFAULT_OPPONENT_WALL_ID else DEFAULT_EQUIPPED_WALL_ID
            return if (userPlayerIndex == 1) {
                Pair(alternateSkin, s1)
            } else {
                Pair(s0, alternateSkin)
            }
        }

        return Pair(s0, s1)
    }

    fun getWallSkinById(id: String): WallSkin {
        val norm = normalize(id)
        return ALL_WALL_SKINS.find { it.id == id || normalize(it.id) == norm } ?: DEFAULT_BLUE
    }

    @DrawableRes
    fun getWallDrawableRes(id: String?, fallbackRes: Int = R.drawable.ic_blue_wall): Int {
        if (id.isNullOrBlank()) return fallbackRes
        val skin = ALL_WALL_SKINS.find { it.id == id || normalize(it.id) == normalize(id) }
        return skin?.drawableResId ?: fallbackRes
    }
}
