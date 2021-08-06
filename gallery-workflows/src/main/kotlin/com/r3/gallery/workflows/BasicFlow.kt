package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.EmptyContract
import com.r3.gallery.states.EmptyState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class BasicFlowInitiator(private val participant: Party) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val output = EmptyState(listOf(participant, ourIdentity))

        val transactionBuilder = TransactionBuilder(notary)
            .addCommand(EmptyContract.Commands.Create(), participant.owningKey, ourIdentity.owningKey)
            .addOutputState(output)

        transactionBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)

        val counterPartySession = listOf(initiateFlow(participant))
        val stx = subFlow(CollectSignaturesFlow(ptx, counterPartySession))

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

