package com.wallwar.data.billing

object BillingConstants {
    // In-App Consumable Coin Packages
    const val COINS_PACK_100 = "coins_pack_100"
    const val COINS_PACK_300 = "coins_pack_300"
    const val COINS_PACK_600 = "coins_pack_600"
    const val COINS_PACK_1300 = "coins_pack_1300"
    const val COINS_PACK_3000 = "coins_pack_3000"
    const val COINS_PACK_7500 = "coins_pack_7500"

    // All In-App Product IDs to query from Google Play Store
    val ALL_IN_APP_PRODUCT_IDS = listOf(
        COINS_PACK_100,
        COINS_PACK_300,
        COINS_PACK_600,
        COINS_PACK_1300,
        COINS_PACK_3000,
        COINS_PACK_7500
    )

    // Product metadata definition
    data class CoinPackDefinition(
        val productId: String,
        val legacyId: String,
        val nameEn: String,
        val coins: Int,
        val defaultPriceUsd: String,
        val defaultPriceToman: String,
        val popularTag: String? = null
    )

    val DEFINITIONS = listOf(
        CoinPackDefinition(COINS_PACK_100, "micro", "Micro Pack", 100, "$0.99", "10,000 T"),
        CoinPackDefinition(COINS_PACK_300, "starter", "Starter Pack", 300, "$2.49", "29,000 T"),
        CoinPackDefinition(COINS_PACK_600, "gamer", "Gamer Pack", 600, "$4.99", "58,000 T"),
        CoinPackDefinition(COINS_PACK_1300, "pro", "Pro Pack", 1300, "$8.99", "129,000 T", popularTag = "POPULAR"),
        CoinPackDefinition(COINS_PACK_3000, "master", "Master Pack", 3000, "$17.99", "299,000 T", popularTag = "GREAT VALUE"),
        CoinPackDefinition(COINS_PACK_7500, "champion", "Champion Vault", 7500, "$39.99", "748,000 T", popularTag = "BEST VALUE")
    )

    fun getTomanPriceForCoins(coins: Int): String {
        return when (coins) {
            100 -> "10,000 T"
            300 -> "29,000 T"
            600 -> "58,000 T"
            1300 -> "129,000 T"
            3000 -> "299,000 T"
            7500 -> "748,000 T"
            else -> when {
                coins <= 100 -> "10,000 T"
                coins <= 300 -> "29,000 T"
                coins <= 600 -> "58,000 T"
                coins <= 1300 -> "129,000 T"
                coins <= 3000 -> "299,000 T"
                else -> "748,000 T"
            }
        }
    }

    fun getTomanPriceForProductId(productId: String): String {
        val def = getDefinitionByProductId(productId)
        return def?.defaultPriceToman ?: getTomanPriceForCoins(getCoinsForProductId(productId))
    }

    fun getDefinitionByProductId(productId: String): CoinPackDefinition? {
        return DEFINITIONS.find { it.productId == productId || it.legacyId == productId }
    }

    fun getCoinsForProductId(productId: String): Int {
        return getDefinitionByProductId(productId)?.coins ?: 100
    }

    fun getCanonicalProductId(id: String): String {
        return when (id) {
            "micro", COINS_PACK_100 -> COINS_PACK_100
            "starter", COINS_PACK_300 -> COINS_PACK_300
            "gamer", COINS_PACK_600 -> COINS_PACK_600
            "pro", COINS_PACK_1300 -> COINS_PACK_1300
            "master", COINS_PACK_3000 -> COINS_PACK_3000
            "champion", COINS_PACK_7500 -> COINS_PACK_7500
            else -> id
        }
    }
}
