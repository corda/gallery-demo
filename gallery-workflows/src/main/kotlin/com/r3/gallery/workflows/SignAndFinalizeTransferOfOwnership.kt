package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.workflows.internal.CollectSignaturesInitiatingFlow
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction

/**
 * Sign and finalise the unsigned swap push [WireTransaction] and return a [SignedTransaction].
 * @property wireTransaction transaction to sign and finalise.
 */
@StartableByRPC
@InitiatingFlow
class SignAndFinalizeTransferOfOwnership(
    private val wireTransaction: WireTransaction) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val tx = wireTransaction.toTransactionBuilder()
        val locallySignedTx = serviceHub.signInitialTransaction(tx, ourIdentity.owningKey)

        val otherParticipants = tx.outputStates().flatMap { it.data.participants }
            .distinct()
            .mapNotNull {
                serviceHub.identityService.wellKnownPartyFromAnonymous(it)
            }
            .filter { it != ourIdentity }

        val otherSigners = tx.commands().flatMap { it.signers }
            .distinct()
            .mapNotNull {
                serviceHub.identityService.partyFromKey(it)
            }
            .filter { it != ourIdentity }

        val signedTx = subFlow(CollectSignaturesInitiatingFlow(locallySignedTx, otherSigners))

        val sessions = otherParticipants.map { initiateFlow(it) }
        return subFlow(FinalityFlow(signedTx, sessions))
    }

    /**
     * Convert this [WireTransaction] into a [TransactionBuilder] instance.
     * @return [TransactionBuilder] instance.
     */
    @Suspendable
    private fun WireTransaction.toTransactionBuilder(): TransactionBuilder {
        return TransactionBuilder(
            notary = this.notary!!,
            inputs = this.inputs.toMutableList(),
            attachments = this.attachments.toMutableList(),
            outputs = this.outputs.toMutableList(),
            commands = this.commands.toMutableList(),
            window = this.timeWindow,
            privacySalt = this.privacySalt,
            references = this.references.toMutableList(),
            serviceHub = serviceHub)
    }
}

/**
 * Responder flow for [SignAndFinalizeTransferOfOwnership].
 * Sign and finalise the swap push transaction.
 */
@InitiatedBy(SignAndFinalizeTransferOfOwnership::class)
class SignAndFinaliseTxForPushHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            //subFlow(ObserverAwareFinalityFlowHandler(otherSession))
            subFlow(ReceiveFinalityFlow(otherSession))
        }
    }
}