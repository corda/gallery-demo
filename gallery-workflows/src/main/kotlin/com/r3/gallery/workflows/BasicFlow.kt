package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.EmptyContract
import com.r3.gallery.states.EmptyState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
@InitiatingFlow
class BasicFlowInitiator(private val participant: Party) : FlowLogic<SignedTransaction>() {

    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new IOU.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val output = EmptyState(listOf(participant, ourIdentity))

        progressTracker.currentStep = GENERATING_TRANSACTION

        val transactionBuilder = TransactionBuilder(notary)
            .addCommand(EmptyContract.Commands.Create(), participant.owningKey, ourIdentity.owningKey)
            .addOutputState(output)

        progressTracker.currentStep = VERIFYING_TRANSACTION

        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)

        progressTracker.currentStep = GATHERING_SIGS
        val counterPartySession = listOf(initiateFlow(participant))
        val stx = subFlow(CollectSignaturesFlow(ptx, counterPartySession))

        progressTracker.currentStep = FINALISING_TRANSACTION
        return subFlow(FinalityFlow(stx, counterPartySession))
    }

}

@InitiatedBy(BasicFlowInitiator::class)
class BasicFlowResponder(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val stx = subFlow(object : SignTransactionFlow(counterPartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // no additional checks
            }
        })

        subFlow(ReceiveFinalityFlow(counterPartySession, stx.id))
    }

}