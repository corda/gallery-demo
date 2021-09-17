package com.r3.gallery.workflows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.redeem.addTokensToRedeem
import com.r3.corda.lib.tokens.workflows.utilities.tokenAmountsByToken
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import com.r3.gallery.utils.AuctionCurrency
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@StartableByRPC
class BurnTokens(private val currency: String = "GBP") : FlowLogic<Unit>() {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BurnTokens::class.java)
    }

    @Suspendable
    override fun call() {
        val tokenType = if (currency.equals("GBP", true)) AuctionCurrency.getInstance("GBP")
            else  AuctionCurrency.getInstance("CBDC")

        // TODO search for encumbered tokens, request creator to override and burn

        val tokens = serviceHub.vaultService.tokenAmountsByToken(tokenType).states
        if (tokens.isNotEmpty()) {
            val txBuilder =  TransactionBuilder(tokens.first().state.notary) // network only has one notary
            addTokensToRedeem(txBuilder, tokens)
            txBuilder.verify(serviceHub)
            val stx = serviceHub.signInitialTransaction(txBuilder)
            subFlow(FinalityFlow(stx, emptyList()))
            logger.info(
                "Redeemed the following tokens: ${serviceHub.vaultService.tokenBalance(tokenType)} on $ourIdentity"
            )
        }

        logger.info("No tokens needed redemption on $ourIdentity")
    }
}