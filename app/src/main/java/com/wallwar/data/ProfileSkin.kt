package com.wallwar.data

data class ProfileSkin(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val priceCoins: Int,
    val symbol: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val tag: String,
    val isDefault: Boolean = false
) {
    val coinPrice: Int get() = priceCoins
}

object ProfileSkinCatalog {
    val DEFAULT_SKIN = ProfileSkin(
        id = "skin_default",
        name = "Classic Duelist",
        title = "Novice Grid Fighter",
        description = "The standard combat suit equipped by all initial grid duelists.",
        priceCoins = 0,
        symbol = "DUELIST",
        primaryColorHex = 0xFF00E5FF,
        secondaryColorHex = 0xFF121829,
        tag = "Standard Issue",
        isDefault = true
    )

    val ALL_SKINS: List<ProfileSkin> = listOf(
        DEFAULT_SKIN,
        // 1. 1000 Coins
        ProfileSkin(
            id = "skin_cyber_ninja",
            name = "Cyber Shinobi",
            title = "Neon Infiltrator",
            description = "Silent stealth specialist trained to slip through narrow wall corridors.",
            priceCoins = 1000,
            symbol = "SHINOBI",
            primaryColorHex = 0xFF00E5FF, // Cyan
            secondaryColorHex = 0xFF0D47A1, // Deep Blue
            tag = "Tier 1 · Infiltrator"
        ),
        // 2. 1500 Coins
        ProfileSkin(
            id = "skin_neon_knight",
            name = "Neon Paladin",
            title = "Grid Defender",
            description = "Shielded in electromagnetic forcefields to withstand high-stakes duels.",
            priceCoins = 1500,
            symbol = "PALADIN",
            primaryColorHex = 0xFF00E676, // Emerald
            secondaryColorHex = 0xFF004D40,
            tag = "Tier 1 · Vanguard"
        ),
        // 3. 2000 Coins
        ProfileSkin(
            id = "skin_ronin_ghost",
            name = "Ronin Ghost",
            title = "Blade of the Grid",
            description = "A lone mercenary whose swift tactics cut through enemy blockade lines.",
            priceCoins = 2000,
            symbol = "RONIN",
            primaryColorHex = 0xFFFF5252, // Crimson
            secondaryColorHex = 0xFF3E2723,
            tag = "Tier 2 · Ronin"
        ),
        // 4. 3000 Coins
        ProfileSkin(
            id = "skin_neo_valkyrie",
            name = "Neo Valkyrie",
            title = "Celestial Guardian",
            description = "Armed with divine photon wings and impenetrable perimeter protocols.",
            priceCoins = 3000,
            symbol = "VALKYRIE",
            primaryColorHex = 0xFFFFD700, // Gold
            secondaryColorHex = 0xFFFF6D00,
            tag = "Tier 2 · Valkyrie"
        ),
        // 5. 4000 Coins
        ProfileSkin(
            id = "skin_void_phantom",
            name = "Void Phantom",
            title = "Abyssal Duelist",
            description = "Manifests from the dark realm, distorting the battlefield matrix.",
            priceCoins = 4000,
            symbol = "PHANTOM",
            primaryColorHex = 0xFFD500F9, // Electric Violet
            secondaryColorHex = 0xFF4A148C,
            tag = "Tier 3 · Phantom"
        ),
        // 6. 5000 Coins
        ProfileSkin(
            id = "skin_mecha_titan",
            name = "Mecha Titan",
            title = "Heavy Cyber Mech",
            description = "Titanium-armored juggernaut engineered for total grid conquest.",
            priceCoins = 5000,
            symbol = "TITAN",
            primaryColorHex = 0xFF64FFDA, // Teal Chrome
            secondaryColorHex = 0xFF263238,
            tag = "Tier 3 · Cyborg"
        ),
        // 7. 6000 Coins
        ProfileSkin(
            id = "skin_draco_cyberlord",
            name = "Draco Cyberlord",
            title = "Plasma Dragon",
            description = "Legendary beast radiating concentrated plasma flames across every barrier.",
            priceCoins = 6000,
            symbol = "DRACO",
            primaryColorHex = 0xFFFF3D00, // Plasma Orange
            secondaryColorHex = 0xFF880E4F,
            tag = "Tier 4 · Mythic"
        ),
        // 8. 7000 Coins
        ProfileSkin(
            id = "skin_quantum_archon",
            name = "Quantum Archon",
            title = "Space-Time Ruler",
            description = "Controls temporal reality, calculating winning paths before walls fall.",
            priceCoins = 7000,
            symbol = "ARCHON",
            primaryColorHex = 0xFF7C4DFF, // Deep Indigo
            secondaryColorHex = 0xFF311B92,
            tag = "Tier 4 · Cosmic"
        ),
        // 9. 8000 Coins
        ProfileSkin(
            id = "skin_astral_phoenix",
            name = "Astral Phoenix",
            title = "Solar Rebirth",
            description = "Reborn from supernova flames, guaranteeing unending competitive dominance.",
            priceCoins = 8000,
            symbol = "PHOENIX",
            primaryColorHex = 0xFFFFAB00, // Solar Flare
            secondaryColorHex = 0xFFDD2C00,
            tag = "Tier 5 · Legendary"
        ),
        // 10. 9000 Coins
        ProfileSkin(
            id = "skin_apex_overlord",
            name = "Apex Overlord",
            title = "Sovereign of Wall War",
            description = "The ultimate cyber crown. Unrivaled supreme champion of the arena.",
            priceCoins = 9000,
            symbol = "OVERLORD",
            primaryColorHex = 0xFFFFD700, // Crown Gold
            secondaryColorHex = 0xFF6200EA, // Royal Purple
            tag = "Tier 5 · Supreme"
        )
    )

    val DEFAULT_UNLOCKED_SKIN_IDS: Set<String> = setOf("skin_default")

    fun getSkinById(id: String?): ProfileSkin? {
        if (id == null) return null
        val cleanId = id.removePrefix("skin:")
        return ALL_SKINS.find { it.id == cleanId || it.id == id }
    }

    fun isSkinUrl(photoUrl: String?): Boolean {
        if (photoUrl.isNullOrBlank()) return false
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) return false
        return photoUrl.startsWith("skin:") || ALL_SKINS.any { it.id == photoUrl }
    }
}
