package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.gallery.contracts.LockContract
import com.r3.gallery.states.LockState
import com.r3.gallery.utils.addMoveTokens
import net.corda.core.contracts.Command
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Unlock the token states encumbered by the transaction identified by [encumberedTxHash].
 * @property encumberedTxHash the TX ID of the encumbered token offer transaction
 * @property notarySignature the [TransactionSignature] required to unlock the lock state.
 */
@StartableByRPC
@InitiatingFlow
class UnlockEncumberedTokensFlow(
    private val encumberedTxHash: SecureHash,
    private val notarySignature: TransactionSignature
) : FlowLogic<SignedTransaction>() {

    @Suppress("ClassName")
    companion object {
        object GATHERING_TRANSACTION :
            ProgressTracker.Step("Gathering encumbered transaction to unlock based on transaction's hash.")

        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on unlock parameters.")
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

        val lockStates = encumberedTx.coreTransaction.outRefsOfType<LockState>().single()
        val tokensStates = encumberedTx.coreTransaction.outRefsOfType<FungibleToken>().filter {
            val party = serviceHub.identityService.requireWellKnownPartyFromAnonymous(it.state.data.holder)
            party == ourIdentity && it.state.encumbrance != null
        }

        val outputStates = tokensStates.map { it.state.data.withNewHolder(ourIdentity) }

        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = TransactionBuilder(notary = encumberedTx.notary!!)
            .addMoveTokens(tokensStates, outputStates, listOf())
            .addInputState(lockStates)
            .addCommand(Command(LockContract.Release(notarySignature), ourIdentity.owningKey))

        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val selfSignedTx = serviceHub.signInitialTransaction(txBuilder)

        progressTracker.currentStep = FINALISING_TRANSACTION
        val sessions = lockStates.state.data.participants.filter { it != ourIdentity }.map { initiateFlow(it) }
        return subFlow(
            FinalityFlow(
                selfSignedTx,
                sessions,
                statesToRecord = StatesToRecord.ALL_VISIBLE,
                FINALISING_TRANSACTION.childProgressTracker()
            )
        )
    }
}

/**
 * Responder flow for [UnlockEncumberedTokensFlow].
 * Sign and finalise the token state unlock transaction.
 */
@InitiatedBy(UnlockEncumberedTokensFlow::class)
class UnlockEncumberedTokensFlowHandler(private val otherSession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    override fun call(): SignedTransaction? {
        return if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            subFlow(ReceiveFinalityFlow(otherSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        } else null
    }
}