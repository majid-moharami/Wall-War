package com.wallwar.data

import androidx.annotation.DrawableRes
import com.wallwar.R

data class BallSkin(
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

object BallSkinCatalog {
    // 0. Default Free Balls (Level 1)
    val DEFAULT_BLUE = BallSkin(
        id = "ball_blue",
        name = "Cyber Blue",
        drawableResId = R.drawable.ic_blue_ball,
        priceCoins = 0,
        description = "Standard issue high-voltage cyber blue duelist ball.",
        rarity = "Starter",
        tag = "Free Default",
        isDefaultUnlocked = true,
        requiredLevel = 1
    )

    val DEFAULT_RED = BallSkin(
        id = "ball_red",
        name = "Crimson Red",
        drawableResId = R.drawable.ic_red_ball,
        priceCoins = 0,
        description = "Aggressive crimson charge combat sphere.",
        rarity = "Starter",
        tag = "Free Default",
        isDefaultUnlocked = true,
        requiredLevel = 1
    )

    // 18 Paid Ball Skins (Levels 5 to 20)
    val ALL_BALL_SKINS: List<BallSkin> = listOf(
        DEFAULT_BLUE,
        DEFAULT_RED,
        // 1) 2,500 coins - Level 5
        BallSkin(
            id = "ball_tennis",
            name = "Tennis Ball",
            drawableResId = R.drawable.ic_tennis_ball,
            priceCoins = 2500,
            description = "High-velocity neon court felt ball with energetic bounce dynamics.",
            rarity = "Common",
            tag = "Sports",
            requiredLevel = 5
        ),
        // 2) 3,000 coins - Level 6
        BallSkin(
            id = "ball_8ball",
            name = "8-Ball",
            drawableResId = R.drawable.ic_8ball,
            priceCoins = 3000,
            description = "The classic polished black eight-ball for precise tactical duelists.",
            rarity = "Common",
            tag = "Billiards",
            requiredLevel = 6
        ),
        // 3) 3,800 coins - Level 7
        BallSkin(
            id = "ball_zombie_eye",
            name = "Zombie Eye",
            drawableResId = R.drawable.ic_zombie_eye_ball,
            priceCoins = 3800,
            description = "An eerie undead ocular sphere tracking enemy steps across corridors.",
            rarity = "Uncommon",
            tag = "Spooky",
            requiredLevel = 7
        ),
        // 4) 5,000 coins - Level 8
        BallSkin(
            id = "ball_pixel_orb",
            name = "Pixel Orb",
            drawableResId = R.drawable.ic_pixel_orb_ball,
            priceCoins = 5000,
            description = "Retro 8-bit arcade aesthetic glowing with nostalgic matrix energy.",
            rarity = "Uncommon",
            tag = "Retro Arcade",
            requiredLevel = 8
        ),
        // 5) 6,500 coins - Level 9
        BallSkin(
            id = "ball_basketball",
            name = "Basketball",
            drawableResId = R.drawable.ic_basketball_ball,
            priceCoins = 6500,
            description = "Street-court leather ball engineered for high-stakes crossovers.",
            rarity = "Uncommon",
            tag = "Sports",
            requiredLevel = 9
        ),
        // 6) 8,000 coins - Level 10
        BallSkin(
            id = "ball_neon_core",
            name = "Neon Core",
            drawableResId = R.drawable.ic_neon_core_ball,
            priceCoins = 8000,
            description = "Hyper-charged quantum capacitor radiating pure luminescent cyan energy.",
            rarity = "Rare",
            tag = "Cybernetic",
            requiredLevel = 10
        ),
        // 7) 10,000 coins - Level 11
        BallSkin(
            id = "ball_toxic_slime",
            name = "Toxic Slime",
            drawableResId = R.drawable.ic_toxic_slime_ball,
            priceCoins = 10000,
            description = "Radioactive bio-luminescent ooze sphere that leaves a hazardous aura.",
            rarity = "Rare",
            tag = "Biohazard",
            requiredLevel = 11
        ),
        // 8) 11,000 coins - Level 12
        BallSkin(
            id = "ball_magma_sphere",
            name = "Magma Sphere",
            drawableResId = R.drawable.ic_magma_sphere_ball,
            priceCoins = 11000,
            description = "Molten subterranean volcanic rock churning with searing lava cracks.",
            rarity = "Rare",
            tag = "Elemental",
            requiredLevel = 12
        ),
        // 9) 12,500 coins - Level 13
        BallSkin(
            id = "ball_cyberpunk_pulse",
            name = "Cyberpunk Pulse",
            drawableResId = R.drawable.ic_cyberpunk_pulse_ball,
            priceCoins = 12500,
            description = "Futuristic synthwave neon orb throbbing to dynamic cyber frequencies.",
            rarity = "Epic",
            tag = "Synthwave",
            requiredLevel = 13
        ),
        // 10) 13,000 coins - Level 14
        BallSkin(
            id = "ball_cyber_shuriken",
            name = "Cyber Shuriken",
            drawableResId = R.drawable.ic_cyber_shuriken_ball,
            priceCoins = 13000,
            description = "A spinning chrome-edged ninja disc cutting effortlessly through wall grids.",
            rarity = "Epic",
            tag = "Shinobi",
            requiredLevel = 14
        ),
        // 11) 15,000 coins - Level 15
        BallSkin(
            id = "ball_disco",
            name = "Disco Ball",
            drawableResId = R.drawable.ic_disco_ball,
            priceCoins = 15000,
            description = "Mirror-faceted glitter sphere sparkling under intense arena stadium spotlights.",
            rarity = "Epic",
            tag = "Party",
            requiredLevel = 15
        ),
        // 12) 18,000 coins - Level 16
        BallSkin(
            id = "ball_plasma_orb",
            name = "Plasma Orb",
            drawableResId = R.drawable.ic_plasma_orb_ball,
            priceCoins = 18000,
            description = "Ionized plasma sphere charged with high-voltage electromagnetic lightning.",
            rarity = "Epic",
            tag = "Electromagnetic",
            requiredLevel = 16
        ),
        // 13) 25,000 coins - Level 17
        BallSkin(
            id = "ball_black_hole",
            name = "Black Hole",
            drawableResId = R.drawable.ic_black_hole_ball,
            priceCoins = 25000,
            description = "Dense cosmic gravitational singularity warping the fabric of the game board.",
            rarity = "Legendary",
            tag = "Cosmic Void",
            requiredLevel = 17
        ),
        // 14) 26,000 coins - Level 18
        BallSkin(
            id = "ball_solar_flare",
            name = "Solar Flare",
            drawableResId = R.drawable.ic_solar_flare_ball,
            priceCoins = 26000,
            description = "A miniature thermonuclear star erupting with blazing coronal loops.",
            rarity = "Legendary",
            tag = "Stellar",
            requiredLevel = 18
        ),
        // 15) 35,000 coins - Level 19
        BallSkin(
            id = "ball_phoenix_core",
            name = "Phoenix Core",
            drawableResId = R.drawable.ic_phonix_core_ball,
            priceCoins = 35000,
            description = "The undying sacred fiery heart of the reborn mythical phoenix bird.",
            rarity = "Legendary",
            tag = "Mythic",
            requiredLevel = 19
        ),
        // 16) 40,000 coins - Level 19
        BallSkin(
            id = "ball_dragon_eye",
            name = "Dragon Eye",
            drawableResId = R.drawable.ic_dragon_eye_ball,
            priceCoins = 40000,
            description = "Ancient draconic gaze piercing enemy defenses with legendary reptilian intensity.",
            rarity = "Mythic",
            tag = "Dragon",
            requiredLevel = 19
        ),
        // 17) 50,000 coins - Level 20
        BallSkin(
            id = "ball_galaxy_void",
            name = "Galaxy Void",
            drawableResId = R.drawable.ic_galaxy_void_ball,
            priceCoins = 50000,
            description = "A swirling deep-space galaxy holding millions of astral stars within its core.",
            rarity = "Mythic",
            tag = "Celestial",
            requiredLevel = 20
        ),
        // 18) 65,000 coins - Level 20
        BallSkin(
            id = "ball_crown_royalty",
            name = "Crown Royalty",
            drawableResId = R.drawable.ic_crwon_royalty_ball,
            priceCoins = 65000,
            description = "The ultimate supreme crowned orb forged for undisputed rulers of the grid.",
            rarity = "Exalted",
            tag = "Imperial King",
            requiredLevel = 20
        )
    ).sortedWith(compareBy<BallSkin> { it.requiredLevel }.thenBy { it.priceCoins })

    val DEFAULT_UNLOCKED_BALL_IDS: Set<String> = setOf("ball_blue", "ball_red")
    const val DEFAULT_EQUIPPED_BALL_ID = "ball_blue"
    const val DEFAULT_OPPONENT_BALL_ID = "ball_red"

    private fun normalize(id: String): String {
        return id.lowercase().trim()
            .removePrefix("ball_")
            .removePrefix("ic_")
            .removeSuffix("_ball")
    }

    fun getBallSkinById(id: String): BallSkin {
        val norm = normalize(id)
        return ALL_BALL_SKINS.find { it.id == id || normalize(it.id) == norm } ?: DEFAULT_BLUE
    }

    @DrawableRes
    fun getDrawableResId(skinId: String?, fallback: Int = R.drawable.ic_blue_ball): Int {
        if (skinId.isNullOrBlank()) return fallback
        val norm = normalize(skinId)
        if (norm == "red") return R.drawable.ic_red_ball
        if (norm == "blue") return R.drawable.ic_blue_ball
        val found = ALL_BALL_SKINS.find { it.id == skinId || normalize(it.id) == norm }
        return found?.drawableResId ?: fallback
    }

    @DrawableRes
    fun getBallDrawableRes(skinId: String?, defaultRes: Int = R.drawable.ic_blue_ball): Int {
        return getDrawableResId(skinId, defaultRes)
    }
}
