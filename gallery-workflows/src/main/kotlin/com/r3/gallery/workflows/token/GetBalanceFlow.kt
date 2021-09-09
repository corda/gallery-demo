package com.r3.gallery.workflows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

@InitiatingFlow
@StartableByRPC
class GetBalanceFlow(val currency: String) : FlowLogic<Amount<TokenType>>() {

    constructor(currencyTokenType: TokenType) : this(currencyTokenType.tokenIdentifier)

    @Suspendable
    override fun call(): Amount<TokenType> {

        val currencyTokenType: TokenType = FiatCurrency.getInstance(currency)

        return serviceHub.vaultService.tokenBalance(currencyTokenType)
    }
}