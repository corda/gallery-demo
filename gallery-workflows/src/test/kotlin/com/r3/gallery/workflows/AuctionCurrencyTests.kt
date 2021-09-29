package com.r3.gallery.workflows

import com.r3.gallery.utils.AuctionCurrency
import org.junit.Assert.assertEquals
import org.junit.Test

class AuctionCurrencyTests {

    @Test
    fun `can instantiate GBP currency`() {
        val token = AuctionCurrency.getInstance("GBP")

        assertEquals("GBP", token.tokenIdentifier)
    }

    @Test
    fun `can instantiate CBDC currency`() {
        val token = AuctionCurrency.getInstance("CBDC")

        assertEquals("CBDC", token.tokenIdentifier)
    }
}

