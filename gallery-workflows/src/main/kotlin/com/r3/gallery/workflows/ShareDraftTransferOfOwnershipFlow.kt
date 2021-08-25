package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap


@InitiatingFlow
@StartableByRPC
class ShareDraftTransferOfOwnershipFlow(val artworkId: UniqueIdentifier, val bidder: Party) : FlowLogic<Boolean>() {
    override val progressTracker = ProgressTracker()
    private companion object {
        private val cache = mutableMapOf<SecureHash, WireTransaction>()
    }
    @Suspendable
    override fun call(): Boolean {
        val session = initiateFlow(bidder)
        val draftTx = subFlow(BuildDraftTransferOfOwnership(artworkId, bidder))
        session.send(draftTx)
        val draftTxDependencies = draftTx.getDependencies()
        draftTxDependencies.forEach {
            val validatedTxDependency = serviceHub.validatedTransactions.getTransaction(it)
            if (validatedTxDependency == null) {
                FlowException("Unable to find validated transaction for input: $it")
            }
            subFlow(SendTransactionFlow(session, validatedTxDependency!!))
        }
        return session.receive<Boolean>().unwrap { it }
    }
}

@InitiatedBy(ShareDraftTransferOfOwnershipFlow::class)
class ShareDraftTransferOfOwnershipFlowHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): Unit {
        val draftTx = otherSession.receive<WireTransaction>().unwrap { it }
        val draftTxMerkleTree = draftTx.generateWireTransactionMerkleTree()
        val txOk = receiveAndVerifyDependencies(draftTx) &&
                verifyShareConditions(draftTx, draftTxMerkleTree) &&
                verifySharedTx(draftTx) /*&&
                persistTxDetails(draftTx)*/

        otherSession.send(txOk)
    }

    // get and verify all the dependencies of the supplied WireTransaction
    @Suspendable
    private fun receiveAndVerifyDependencies(wireTransaction: WireTransaction) : Boolean {
        val expectedTxs = wireTransaction.getDependencies()
        return expectedTxs.all {
            try {
                subFlow(ReceiveTransactionFlow(otherSession))
                true
            } catch (e: Exception) {
                logger.warn("Failed to resolve input transaction ${it.toHexString()}: ${e.message}")
                false
            }
        }
    }

    // verify conditions which need to be true for this share to be valid
    @Suspendable
    private fun verifyShareConditions(wireTransaction: WireTransaction, expectedMerkleTree: MerkleTree) : Boolean {
        val id = wireTransaction.id
        val suppliedMerkleTree = wireTransaction.merkleTree
        val timeWindow = wireTransaction.timeWindow
        val notary = wireTransaction.notary

        return !listOf(
                (expectedMerkleTree != suppliedMerkleTree) to
                        "The supplied merkle tree ($suppliedMerkleTree) did not match the expected merkle tree ($expectedMerkleTree)",
                (id != suppliedMerkleTree.hash) to
                        "The supplied merkle tree hash (${suppliedMerkleTree.hash}) did not match the supplied id (${id})",
                (timeWindow == null) to
                        "Time window must be provided",
                (notary == null) to
                        "Notary must be provided"
        ).any {
            if (it.first) {
                logger.warn("Failed to process shared transaction $id: ${it.second}")
            }
            it.first
        }
    }

    // verify the transaction which was supplied
    @Suspendable
    private fun verifySharedTx(wireTransaction: WireTransaction) : Boolean {
        val ledgerTx = wireTransaction.toLedgerTransaction(serviceHub)
        return try {
            ledgerTx.verify()
            true
        } catch (e: Exception) {
            logger.warn("Failed to resolve transaction: ${e.message}")
            false
        }
    }

//    // persist the transaction details
//    @Suspendable
//    private fun persistTxDetails(wireTransaction: WireTransaction) : Boolean {
//        return try {
//            val swapService = serviceHub.cordaService(ProposedSwapTxPersistenceService::class.java)
//            swapService.createProposedSwapSharedTx(wireTransaction)
//
//            true
//        } catch (e: Exception) {
//            logger.warn("Failed to store transaction: ${e.message}")
//            false
//        }
//    }
}