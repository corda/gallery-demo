package com.r3.gallery.workflows.webapp.tokennetwork

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.gallery.api.NetworkBalancesResponse
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class GetBalanceFlow : FlowLogic<NetworkBalancesResponse.Balance>() {

    @Suspendable
    override fun call(): NetworkBalancesResponse.Balance {
        val tokensHeld = serviceHub.vaultService.queryBy(FungibleToken::class.java)
            .states.map { it.state }

        val tokenType: TokenType = tokensHeld.first().data.tokenType

        val availableTokens = tokensHeld.filter { it.encumbrance == null }
        val availableTokensAmount = Amount(availableTokens.sumOf { it.data.amount.quantity }, tokenType)
        val encumberedTokens = tokensHeld.minus(availableTokens)
        val encumberedTokensAmount = Amount(encumberedTokens.sumOf { it.data.amount.quantity }, tokenType)

        return NetworkBalancesResponse.Balance(
            currencyCode = tokenType.tokenIdentifier,
            encumberedFunds = encumberedTokensAmount,
            availableFunds = availableTokensAmount
        )
    }
}