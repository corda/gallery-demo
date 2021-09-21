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
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
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
            lockReceiverSession.send("seller")
            lockReceiverSession.sendAndReceive<Boolean>(lockTxHash).unwrap { it }.also {
                result -> if (!result) throw IllegalStateException("Unable to unlock $lockState")
            }
            lockReceiverSession.close()
        }

        // User should only redeem/burn what they are the holder (not any shared token states)
        val tokens = serviceHub.vaultService.tokenAmountsByToken(tokenType).states.filter {
            it.state.data.holder == ourIdentity
        }

        if (tokens.isNotEmpty()) {
            val notary = tokens.first().state.notary
            val issuer = tokens.first().state.data.issuer
            val txBuilder =  TransactionBuilder(notary)
            addTokensToRedeem(txBuilder, tokens)
            txBuilder.verify(serviceHub)
            var stx = serviceHub.signInitialTransaction(txBuilder)
            if (ourIdentity != issuer) { // fetch issuer signature if we are not the issuer.
                val issuerSession = initiateFlow(issuer)
                issuerSession.send("issuer")
                stx = subFlow(CollectSignatureFlow(stx, issuerSession, issuer.owningKey)).let {
                   stx.plus(it)
                }
                subFlow(FinalityFlow(stx, listOf(issuerSession)))
            } else {
                subFlow(FinalityFlow(stx, emptyList()))
            }
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
        val role = otherSession.receive<String>().unwrap { it }
        when (role) {
            "seller" -> {
                val lockStateTxToRelease = otherSession.receive<SecureHash>().unwrap { it }
                subFlow(RevertEncumberedTokensFlow(lockStateTxToRelease))
                otherSession.send(true)
            }
            "issuer" -> {
                val stx = subFlow(object : SignTransactionFlow(otherSession) {
                    override fun checkTransaction(stx: SignedTransaction) {
                        // no checks just sign
                    }
                })
                subFlow(ReceiveFinalityFlow(otherSession, stx.id, statesToRecord = StatesToRecord.ALL_VISIBLE))
            }
        }
    }
}