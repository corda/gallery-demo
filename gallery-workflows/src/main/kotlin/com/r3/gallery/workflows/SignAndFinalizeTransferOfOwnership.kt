package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.workflows.internal.CollectSignaturesInitiatingFlow
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker

/**
 * Sign and finalise the unsigned swap push [WireTransaction] and return a [SignedTransaction].
 * @property wireTransaction transaction to sign and finalise.
 */
@StartableByRPC
@InitiatingFlow
class SignAndFinalizeTransferOfOwnership(
    private val wireTransaction: WireTransaction) : FlowLogic<SignedTransaction>() {

    @Suppress("ClassName")
    companion object {
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_PARTICIPANTS : ProgressTracker.Step("Gathering transaction participants' identities")
        object GATHERING_SIGNERS : ProgressTracker.Step("Gathering transaction signers' identities")
        object COLLECTING_SIGNATURES : ProgressTracker.Step("Collecting transaction signatures from signers.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }
        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
    }

    override val progressTracker = ProgressTracker(
        VERIFYING_TRANSACTION,
        SIGNING_TRANSACTION,
        FINALISING_TRANSACTION,
        GATHERING_PARTICIPANTS,
        GATHERING_SIGNERS,
        COLLECTING_SIGNATURES,
        FINALISING_TRANSACTION
    )

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = VERIFYING_TRANSACTION
        wireTransaction.toLedgerTransaction(serviceHub).verify()

        progressTracker.currentStep = SIGNING_TRANSACTION
        val tx = wireTransaction.toTransactionBuilder()
        val selfSignedTx = serviceHub.signInitialTransaction(tx, ourIdentity.owningKey)

        progressTracker.currentStep = GATHERING_PARTICIPANTS
        val otherParticipants = tx.outputStates().flatMap { it.data.participants }
            .distinct()
            .mapNotNull {
                serviceHub.identityService.wellKnownPartyFromAnonymous(it)
            }
            .filter { it != ourIdentity }

        progressTracker.currentStep = GATHERING_SIGNERS
        val otherSigners = tx.commands().flatMap { it.signers }
            .distinct()
            .mapNotNull {
                serviceHub.identityService.partyFromKey(it)
            }
            .filter { it != ourIdentity }

        progressTracker.currentStep = COLLECTING_SIGNATURES
        val signedTx = subFlow(CollectSignaturesInitiatingFlow(selfSignedTx, otherSigners))

        progressTracker.currentStep = FINALISING_TRANSACTION
        val sessions = otherParticipants.map { initiateFlow(it) }
        return subFlow(FinalityFlow(signedTx, sessions, FINALISING_TRANSACTION.childProgressTracker()))
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
            subFlow(ReceiveFinalityFlow(otherSession))
        }
    }
}