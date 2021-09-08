//package com.r3.gallery.workflows.webapp.artnetwork.gallery
//
//import co.paralleluniverse.fibers.Suspendable
//import com.r3.gallery.api.ProofOfTransferOfOwnership
//import com.r3.gallery.api.TransactionSignature
//import com.r3.gallery.api.UnsignedArtworkTransferTx
//import com.r3.gallery.workflows.webapp.initiateFlowSessions
//import net.corda.core.flows.FinalityFlow
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.StartableByRPC
//import net.corda.core.serialization.deserialize
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.WireTransaction
//import net.corda.core.utilities.ProgressTracker
//import java.util.*
//
///**
// * Award an artwork to a bidder by signing and notarizing an unsigned art transfer transaction,
// * obtaining a [ProofOfTransferOfOwnership]
// *
// * @return Proof that ownership of the artwork has been transferred.
// */
//@StartableByRPC
//class FinaliseArtworkTransferTx(private val unsignedArtworkTransferTx: UnsignedArtworkTransferTx) : FlowLogic<ProofOfTransferOfOwnership>() {
//
//    companion object {
//        object VERIFYING_UNSIGNED_TRANSACTION : ProgressTracker.Step("Verifying validity of unsigned transaction.")
//        object SIGNING_TRANSACTION : ProgressTracker.Step("Applying signature to transaction.")
//        object NOTARISING_TRANSACTION : ProgressTracker.Step("Notarising and finalising transaction.")
//        object PROOF_OF_OWNERSHIP : ProgressTracker.Step("Generating proof of ownership.")
//
//        fun tracker() = ProgressTracker(
//            VERIFYING_UNSIGNED_TRANSACTION,
//            SIGNING_TRANSACTION,
//            NOTARISING_TRANSACTION
//        )
//    }
//
//    override val progressTracker = tracker()
//
//    @Suspendable
//    override fun call(): ProofOfTransferOfOwnership {
//
//        progressTracker.currentStep = VERIFYING_UNSIGNED_TRANSACTION
//        val ltx =  unsignedArtworkTransferTx.transactionBytes.deserialize<WireTransaction>()
//        ltx.toLedgerTransaction(serviceHub).verify() // check validity of transaction
//
//        progressTracker.currentStep = SIGNING_TRANSACTION
//        val ourSig = serviceHub.createSignature(ltx.buildFilteredTransaction{ true }, ourIdentity.owningKey)
//        val stx = SignedTransaction(ltx, listOf(ourSig))
//
//        progressTracker.currentStep = NOTARISING_TRANSACTION
//        val sessions = initiateFlowSessions(stx)
//        val fsTx = subFlow(FinalityFlow(stx, sessions ?: emptyList()))
//
//        progressTracker.currentStep = PROOF_OF_OWNERSHIP
//        return fsTx.let {
//            val notarySig = it.sigs.first { txSig ->
//                serviceHub.identityService.partyFromKey(txSig.by) == it.notary
//            }
//            ProofOfTransferOfOwnership(
//                transactionId = UUID.randomUUID(),
//                transactionHash = it.id.toString(),
//                previousOwnerSignature = TransactionSignature(ourSig.bytes),
//                notarySignature = TransactionSignature(notarySig.bytes)
//            )
//        }
//    }
//}