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
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
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
) : FlowLogic<SignedTransaction>() {

    @Suppress("ClassName")
    companion object {
        object GENERATING_LOCK :
            ProgressTracker.Step("Generating lock state for encumbered token transaction")

        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating encumbered token transaction")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object COLLECTING_SIGNATURES : ProgressTracker.Step("Collecting transaction signatures from signers.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }
        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
    }

    override val progressTracker = ProgressTracker(
        GENERATING_LOCK,
        GENERATING_TRANSACTION,
        VERIFYING_TRANSACTION,
        SIGNING_TRANSACTION,
        COLLECTING_SIGNATURES,
        FINALISING_TRANSACTION
    )

    @Suspendable
    override fun call(): SignedTransaction {
        val compositeKey = serviceHub.registerCompositeKey(ourIdentity, sellerParty)
        val compositeParty = AnonymousParty(compositeKey)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        progressTracker.currentStep = GENERATING_LOCK
        val lockState = LockState(verifiedDraftTx, ourIdentity, sellerParty)
        val partiesAndAmounts = listOf(PartyAndAmount1(compositeParty, encumberedAmount))

        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = try {
            with(TransactionBuilder(notary = notary)) {
                addMoveTokens(
                    serviceHub,
                    partiesAndAmounts,
                    ourIdentity,
                    listOf(sellerParty).map { it.owningKey },
                    lockState
                )
                setTimeWindow(TimeWindow.untilOnly(verifiedDraftTx.timeWindow.untilTime!!.plusSeconds(30)))
            }
        } catch (e: InsufficientBalanceException) {
            throw FlowException("Offered amount ($encumberedAmount) exceeds balance", e)
        }

        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val selfSignedTx = serviceHub.signInitialTransaction(txBuilder, listOf(ourIdentity.owningKey))

        progressTracker.currentStep = COLLECTING_SIGNATURES
        val signedTx = subFlow(CollectSignaturesForComposites(selfSignedTx, listOf(sellerParty)))

        progressTracker.currentStep = FINALISING_TRANSACTION
        val sessions = listOf(initiateFlow(sellerParty))
        return subFlow(FinalityFlow(signedTx, sessions, FINALISING_TRANSACTION.childProgressTracker()))
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
