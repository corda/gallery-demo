package com.r3.corda.lib.tokens.workflows.swaps

import CollectSignaturesInitiatingFlow
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.gallery.states.LockState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction

/**
 * Sign and finalise the unsigned swap push [WireTransaction] and return a [SignedTransaction].
 * @property lockStateAndRef [StateAndRef] with [LockState] containing the controlling notary required to unlock the
 * lock state.
 * @property wireTransaction transaction to sign and finalise.
 */
@StartableByService
@InitiatingFlow
class SignAndFinaliseTxForPush(
        private val lockStateAndRef: StateAndRef<LockState>,
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

        val sessions = otherParticipants.filter { it != ourIdentity }.map { initiateFlow(it) }
        return subFlow(ObserverAwareFinalityFlow(signedTx, sessions))
    }

    /**
     * Convert this [WireTransaction] into a [TransactionBuilder] instance.
     * @return [TransactionBuilder] instance.
     */
    @Suspendable
    private fun WireTransaction.toTransactionBuilder(): TransactionBuilder {
        return TransactionBuilder(
            notary = lockStateAndRef.state.data.controllingNotary,
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
 * Responder flow for [SignAndFinaliseTxForPush].
 * Sign and finalise the swap push transaction.
 */
@InitiatedBy(SignAndFinaliseTxForPush::class)
class SignAndFinaliseTxForPushHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            subFlow(ObserverAwareFinalityFlowHandler(otherSession))
        }
    }
}