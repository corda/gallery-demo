package com.r3.gallery.workflows.webapp

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.gallery.api.NetworkBalancesResponse
import com.r3.gallery.utils.AuctionCurrency
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class GetBalanceFlow(private val currency: String) : FlowLogic<NetworkBalancesResponse.Balance>() {

    @Suspendable
    override fun call(): NetworkBalancesResponse.Balance {
        // Tokens in ledger also include states from encumbrance where ourIdentity might not be holder.
        val tokensInVault = serviceHub.vaultService.queryBy(FungibleToken::class.java)
            .states.map { it.state }

        val tokenType: TokenType = AuctionCurrency.getInstance(currency)

        // no balance available
        if (tokensInVault.isNullOrEmpty()) {
            return NetworkBalancesResponse.Balance(
                currencyCode = tokenType.tokenIdentifier,
                encumberedFunds = Amount.zero(tokenType),
                availableFunds = Amount.zero(tokenType)
            )
        }

        // filter to available and encumbered
        val availableTokens = tokensInVault.filter { it.data.holder == ourIdentity && it.data.issuer == ourIdentity }
        val availableTokensAmount = if (availableTokens.isNotEmpty()) {
            Amount(availableTokens.sumOf { it.data.amount.quantity }, tokenType)
        } else Amount.zero(tokenType)

        val encumberedTokens = tokensInVault.filter { it.encumbrance != null  && it.data.issuer == ourIdentity }
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