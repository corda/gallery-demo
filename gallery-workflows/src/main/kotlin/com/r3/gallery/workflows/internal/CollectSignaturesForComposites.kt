package com.r3.gallery.workflows.internal

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

/**
 * Collect signatures for the provided [SignedTransaction], from the list of [Party] provided.
 * This is an initiating flow, and is used where some of the required signatures are from
 * [CompositeKey]s. The standard Corda CollectSignaturesFlow will not work in this case.
 * @param stx - the [SignedTransaction] to sign
 * @param signers - the list of signing [Party]s
 */
@InitiatingFlow
internal class CollectSignaturesForComposites(
    private val stx: SignedTransaction,
    private val signers: List<Party>
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // create new sessions to signers and trigger the signing responder flow
        val sessions = signers.map { initiateFlow(it) }

        // We filter out any responses that are not
        // `TransactionSignature`s (i.e. refusals to sign).
        val signatures = sessions
            .map { it.sendAndReceive<Any>(stx).unwrap { data -> data } }
            .filterIsInstance<TransactionSignature>()
        return stx.withAdditionalSignatures(signatures)
    }
}

@InitiatedBy(CollectSignaturesForComposites::class)
internal class CollectSignaturesForCompositesHandler(private val otherPartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        otherPartySession.receive<SignedTransaction>().unwrap { partStx ->
            // TODO: add conditions where we might not sign

            val returnStatus = serviceHub.createSignature(partStx)
            otherPartySession.send(returnStatus)
        }
    }
}