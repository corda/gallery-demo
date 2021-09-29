package com.r3.gallery.utils

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.money.FiatCurrency
import net.corda.core.contracts.Amount

/**
 * The Factory class for the Auction's supported currencies.
 */
class AuctionCurrency {
    companion object {
        private val registry = mapOf(
            Pair("GBP", FiatCurrency.getInstance("GBP")),
            Pair("CBDC", TokenType("CBDC", 0))
        )

        @JvmStatic
        fun getInstance(currencyCode: String): TokenType {
            return registry[currencyCode] ?: throw IllegalArgumentException("$currencyCode doesn't exist.")
        }
    }
}

val CBDC = AuctionCurrency.getInstance("CBDC")
val Int.CBDC: Amount<TokenType> get() = CBDC(this)
fun CBDC(amount: Int): Amount<TokenType> = amount(amount, CBDC)