package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import com.r3.gallery.utils.addMoveTokens
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Reverts an encumbered token offer to its original holder only. This can be initiated by the creator of the offer only
 * after the expiry of the lock and only if the token has not been claimed by the receiver before the expiry of the lock.
 * The receiver of the token offer can execute this flow at any time to return the token to the original token holder.
 * @param encumberedTxHash the hash of the encumbered toke offer transaction ID.
 * @return the token reversal transaction, signed and finalised.
 */
@StartableByRPC
@InitiatingFlow
class RevertEncumberedTokensFlow(private val encumberedTxHash: SecureHash) : FlowLogic<SignedTransaction>() {

    @Suppress("ClassName")
    companion object {
        object GATHERING_TRANSACTION :
            ProgressTracker.Step("Gathering encumbered transaction to revert based on transaction's hash.")

        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on revert parameters.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
    }

    override val progressTracker = ProgressTracker(
        GATHERING_TRANSACTION,
        GENERATING_TRANSACTION,
        VERIFYING_TRANSACTION,
        SIGNING_TRANSACTION,
        FINALISING_TRANSACTION
    )

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = GATHERING_TRANSACTION
        val encumberedTx = serviceHub.validatedTransactions.getTransaction(encumberedTxHash)
            ?: throw IllegalArgumentException("Unable to find transaction with id: $encumberedTxHash")

        val lockState = encumberedTx.coreTransaction.outRefsOfType<LockState>().single()
        val encumberedTxIssuer = lockState.state.data.creator
        val tokensStates: List<StateAndRef<FungibleToken>> = encumberedTx.coreTransaction.outRefsOfType<FungibleToken>().filter {
            val party = serviceHub.identityService.requireWellKnownPartyFromAnonymous(it.state.data.holder)
            party == ourIdentity && it.state.encumbrance != null
        }

        val outputStates = tokensStates.map { it.state.data.withNewHolder(encumberedTxIssuer) }
        // enforce time window on the revert transaction to fall only after the time window to claim the encumbered tokens has expired
        val timeWindowForRevert = TimeWindow.fromOnly(
            lockState.state.data.timeWindow.untilTime!!.plusSeconds(31)
        )
        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = TransactionBuilder(notary = encumberedTx.notary!!)
            .addMoveTokens(tokensStates, outputStates, listOf())
            .addInputState(lockState)
            .addCommand(Command(LockContract.Revert(), ourIdentity.owningKey))
            .setTimeWindow(timeWindowForRevert)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val selfSignedTx = serviceHub.signInitialTransaction(txBuilder)

        progressTracker.currentStep = FINALISING_TRANSACTION
        val sessions = lockState.state.data.participants.filter { it != ourIdentity }.map { initiateFlow(it) }
        return subFlow(FinalityFlow(selfSignedTx, sessions, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}

/**
 * Responder flow for [RevertEncumberedTokensFlow].
 * Sign and finalise the reverted token transaction.
 */
@InitiatedBy(RevertEncumberedTokensFlow::class)
class RevertEncumberedTokensFlowHandler(private val otherSession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    override fun call(): SignedTransaction? {
        return if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            subFlow(ReceiveFinalityFlow(otherSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        } else null
    }
}