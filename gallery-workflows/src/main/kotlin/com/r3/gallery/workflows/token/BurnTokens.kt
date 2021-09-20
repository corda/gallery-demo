package com.r3.gallery.workflows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.redeem.addTokensToRedeem
import com.r3.corda.lib.tokens.workflows.utilities.tokenAmountsByToken
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import com.r3.gallery.states.LockState
import com.r3.gallery.utils.AuctionCurrency
import com.r3.gallery.workflows.RevertEncumberedTokensFlow
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@StartableByRPC
@InitiatingFlow
class BurnTokens(private val currency: String = "GBP") : FlowLogic<Unit>() {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BurnTokens::class.java)
    }

    @Suspendable
    override fun call() {
        val tokenType = if (currency.equals("GBP", true)) AuctionCurrency.getInstance("GBP")
            else  AuctionCurrency.getInstance("CBDC")

        // Request release of any token locks
        serviceHub.vaultService.queryBy(LockState::class.java).states.filter {
            it.state.data.creator == ourIdentity
        }.forEach { lockStateAndRef ->
            val lockState = lockStateAndRef.state.data
            val lockTxHash = lockStateAndRef.ref.txhash
            val lockReceiverSession = initiateFlow(lockState.receiver)

            logger.info("Attempting unlock of $lockState, $lockTxHash")
            lockReceiverSession.sendAndReceive<Boolean>(lockTxHash).unwrap { it }.also {
                result -> if (!result) throw IllegalStateException("Unable to unlock $lockState")
            }
        }

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

@InitiatedBy(BurnTokens::class)
class BurnTokensHandler(private val otherSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val lockStateTxToRelease = otherSession.receive<SecureHash>().unwrap { it }
        subFlow(RevertEncumberedTokensFlow(lockStateTxToRelease))
        otherSession.send(true)
    }
}