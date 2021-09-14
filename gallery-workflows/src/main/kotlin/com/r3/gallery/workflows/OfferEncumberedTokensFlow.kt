package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.gallery.states.LockState
import com.r3.gallery.states.ValidatedDraftTransferOfOwnership
import com.r3.gallery.utils.addMoveTokens
import com.r3.gallery.utils.registerCompositeKey
import com.r3.gallery.workflows.internal.CollectSignaturesForComposites
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.crypto.SecureHash
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
class OfferEncumberedTokensFlow(
    val sellerParty: Party,
    val verifiedDraftTx: ValidatedDraftTransferOfOwnership,
    val encumberedAmount: Amount<TokenType>
) : FlowLogic<SecureHash>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SecureHash {
        val compositeKey = serviceHub.registerCompositeKey(ourIdentity, sellerParty)
        val compositeParty = AnonymousParty(compositeKey)
        val lockState = LockState(verifiedDraftTx, ourIdentity, sellerParty)

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

        return subFlow(FinalityFlow(signedTx, initiateFlow(sellerParty))).id
    }
}

/**
 * Responder flow for [OfferEncumberedTokensFlow].
 * Finalise push token transaction.
 */
@InitiatedBy(OfferEncumberedTokensFlow::class)
class OfferEncumberedTokensFlowHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        serviceHub.registerCompositeKey(ourIdentity, otherSession.counterparty)

        subFlow(
            ReceiveFinalityFlow(
                otherSideSession = otherSession,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}
