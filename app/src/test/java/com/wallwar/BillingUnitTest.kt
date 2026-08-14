package com.wallwar

import com.wallwar.data.billing.BillingConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingUnitTest {

    @Test
    fun testBillingConstantsProductIds() {
        assertEquals(6, BillingConstants.ALL_IN_APP_PRODUCT_IDS.size)
        assertTrue(BillingConstants.ALL_IN_APP_PRODUCT_IDS.contains(BillingConstants.COINS_PACK_100))
        assertTrue(BillingConstants.ALL_IN_APP_PRODUCT_IDS.contains(BillingConstants.COINS_PACK_300))
        assertTrue(BillingConstants.ALL_IN_APP_PRODUCT_IDS.contains(BillingConstants.COINS_PACK_600))
        assertTrue(BillingConstants.ALL_IN_APP_PRODUCT_IDS.contains(BillingConstants.COINS_PACK_1300))
        assertTrue(BillingConstants.ALL_IN_APP_PRODUCT_IDS.contains(BillingConstants.COINS_PACK_3000))
        assertTrue(BillingConstants.ALL_IN_APP_PRODUCT_IDS.contains(BillingConstants.COINS_PACK_7500))
    }

    @Test
    fun testBillingDefinitionsAndCoinAmounts() {
        assertEquals(100, BillingConstants.getCoinsForProductId(BillingConstants.COINS_PACK_100))
        assertEquals(300, BillingConstants.getCoinsForProductId(BillingConstants.COINS_PACK_300))
        assertEquals(600, BillingConstants.getCoinsForProductId(BillingConstants.COINS_PACK_600))
        assertEquals(1300, BillingConstants.getCoinsForProductId(BillingConstants.COINS_PACK_1300))
        assertEquals(3000, BillingConstants.getCoinsForProductId(BillingConstants.COINS_PACK_3000))
        assertEquals(7500, BillingConstants.getCoinsForProductId(BillingConstants.COINS_PACK_7500))
    }

    @Test
    fun testCanonicalIdMapping() {
        assertEquals(BillingConstants.COINS_PACK_100, BillingConstants.getCanonicalProductId("micro"))
        assertEquals(BillingConstants.COINS_PACK_300, BillingConstants.getCanonicalProductId("starter"))
        assertEquals(BillingConstants.COINS_PACK_600, BillingConstants.getCanonicalProductId("gamer"))
        assertEquals(BillingConstants.COINS_PACK_1300, BillingConstants.getCanonicalProductId("pro"))
        assertEquals(BillingConstants.COINS_PACK_3000, BillingConstants.getCanonicalProductId("master"))
        assertEquals(BillingConstants.COINS_PACK_7500, BillingConstants.getCanonicalProductId("champion"))
    }

    @Test
    fun testPackDefinitionsHaveValidMetadata() {
        BillingConstants.DEFINITIONS.forEach { def ->
            assertTrue(def.coins > 0)
            assertTrue(def.nameEn.isNotBlank())
            assertTrue(def.defaultPriceUsd.startsWith("$"))
            assertNotNull(BillingConstants.getDefinitionByProductId(def.productId))
        }
    }
}
