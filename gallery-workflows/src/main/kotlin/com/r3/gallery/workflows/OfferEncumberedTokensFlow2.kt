package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.gallery.states.LockState
import com.r3.gallery.utils.addMoveTokens
import com.r3.gallery.workflows.internal.CollectSignaturesForComposites
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.CompositeKey
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount as PartyAndAmount1

// Some assumptions:
// - all tokens in this transaction are self issued
// - all inputs in this transaction are from the same party

@InitiatingFlow
@StartableByRPC
class OfferEncumberedTokensFlow2(
    val lockState: LockState,
    val sellerParty: Party,
    val encumberedAmount: Amount<TokenType>
) : FlowLogic<StateRef>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): StateRef {
        // Executing on buyer @ token, offers token to seller @ token. The lock state on cbdc-demo was
        // (a @ CN1 + b @ CN2) but supposed to be (a @ CN1 + b' @ CN1), which is equivalent, in our case, to
        // (gallery @ art + buyer @ token) when it should be (gallery @ art + bidder @ art).
        val compositeKey = lockState.getCompositeKey()
        val compositeParty = AnonymousParty(compositeKey)
        serviceHub.identityService.registerKey(compositeKey, ourIdentity)

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
         val txBuilder = try {
            with(TransactionBuilder(notary = notary)) {
                addMoveTokens(
                    serviceHub,
                    listOf(PartyAndAmount1(compositeParty, encumberedAmount)),
                    ourIdentity,
                    listOf(sellerParty).map { it.owningKey },
                    lockState
                )
                setTimeWindow(lockState.timeWindow)
            }
        } catch (e: InsufficientBalanceException) {
            throw FlowException("Offered amount ($encumberedAmount) exceeds balance", e)
        }

        txBuilder.verify(serviceHub)
        var signedTx = serviceHub.signInitialTransaction(txBuilder, listOf(ourIdentity.owningKey))
        // TODO: discuss what "will not be needed for X-Network, as no additional signers! - DELETE"
        signedTx = subFlow(CollectSignaturesForComposites(signedTx, listOf(sellerParty)))

        val stx = subFlow(FinalityFlow(signedTx, initiateFlow(sellerParty)))
        val lockStateRef = stx.tx.outRefsOfType(LockState::class.java).single()

        return lockStateRef.ref
    }
}

/**
 * Responder flow for [OfferEncumberedTokensFlow2].
 * Finalise push token transaction.
 */
@InitiatedBy(OfferEncumberedTokensFlow2::class)
class OfferEncumberedTokensFlow2Handler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val compositeKey = CompositeKey.Builder()
            .addKey(ourIdentity.owningKey, weight = 1)
            .addKey(otherSession.counterparty.owningKey, weight = 1)
            .build(1)

        serviceHub.identityService.registerKey(compositeKey, ourIdentity)

        subFlow(
            ReceiveFinalityFlow(
                otherSideSession = otherSession,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}
