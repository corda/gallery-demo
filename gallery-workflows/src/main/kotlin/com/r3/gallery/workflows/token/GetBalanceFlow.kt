package com.r3.gallery.workflows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import com.r3.gallery.utils.AuctionCurrency
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

/**
 * Get the token balance for the given [currency]
 */
@InitiatingFlow
@StartableByRPC
class GetBalanceFlow(private val currency: String) : FlowLogic<Amount<TokenType>>() {

    constructor(currencyTokenType: TokenType) : this(currencyTokenType.tokenIdentifier)

    @Suspendable
    override fun call(): Amount<TokenType> {

        val currencyTokenType: TokenType = AuctionCurrency.getInstance(currency)

        return serviceHub.vaultService.tokenBalance(currencyTokenType)
    }
}