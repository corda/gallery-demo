package com.r3.gallery.workflows.webapp

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.GBP
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

        val tokenType: TokenType = if (serviceHub.networkMapCache.notaryIdentities.first()
            .name.organisation.contains("GBP")) GBP else TokenType("CBDC", 2)

        // no balance available
        if (tokensHeld.isNullOrEmpty()) {
            return NetworkBalancesResponse.Balance(
                currencyCode = tokenType.tokenIdentifier,
                encumberedFunds = Amount.zero(tokenType),
                availableFunds = Amount.zero(tokenType)
            )
        }

        // filter to available and encumbered
        val availableTokens = tokensHeld.filter { it.encumbrance == null }
        val availableTokensAmount = Amount(availableTokens.sumOf { it.data.amount.quantity }, tokenType)

        val encumberedTokens = tokensHeld.minus(availableTokens)
        val encumberedTokensAmount = if (encumberedTokens.isNotEmpty())
            Amount(encumberedTokens.sumOf { it.data.amount.quantity }, tokenType)
        else Amount.zero(tokenType)

        return NetworkBalancesResponse.Balance(
            currencyCode = tokenType.tokenIdentifier,
            encumberedFunds = encumberedTokensAmount,
            availableFunds = availableTokensAmount
        )
    }
}