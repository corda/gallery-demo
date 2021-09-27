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
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker

/**
 * Offers [encumberedAmount] of a given [TokenType] from the buyer party on the Token Network to a [sellerParty] in
 * the same Token Network. The unsigned [WireTransaction] (and some of its properties) that have been validated by
 * the trusted node on the Art Network iz used to produce the [LockState] encumbrance.
 * @param verifiedDraftTx the [ValidatedDraftTransferOfOwnership] with the art draft transfer from the Art Network.
 * @param sellerParty the [Party] to transfer tokens to.
 * @param encumberedAmount token quantity of [TokenType] to offer.
 * @return the encumbered token transaction, signed and finalised.
 */
@InitiatingFlow
@StartableByRPC
class OfferEncumberedTokensFlow(
    private val sellerParty: Party,
    private val verifiedDraftTx: ValidatedDraftTransferOfOwnership,
    private val encumberedAmount: Amount<TokenType>
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

        // Building a composite key from the two exchanging parties allows eiter party to act as the holder of the
        // encumbered token. The composite key is registered at both ends of the flow as the local node identity via
        // the identity service. Adding the key to the identity service keys allows the finality flow to find the
        // participants and prevents the node from trying to contact other nodes.
        val compositeKey = serviceHub.registerCompositeKey(ourIdentity, sellerParty)
        val compositeHolderParty = AnonymousParty(compositeKey)

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        progressTracker.currentStep = GENERATING_LOCK
        val lockState = LockState(verifiedDraftTx, ourIdentity, sellerParty)

        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = try {
            with(TransactionBuilder(notary = notary)) {
                addMoveTokens(
                    serviceHub,
                    encumberedAmount,
                    compositeHolderParty,
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
 * Finalise the encumbered token offer transaction.
 */
@InitiatedBy(OfferEncumberedTokensFlow::class)
class OfferEncumberedTokensFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
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
