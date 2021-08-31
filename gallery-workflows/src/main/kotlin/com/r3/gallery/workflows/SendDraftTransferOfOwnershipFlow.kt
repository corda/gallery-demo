package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.utils.generateWireTransactionMerkleTree
import com.r3.gallery.utils.getDependencies
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.MerkleTree
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.time.Duration
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class SendDraftTransferOfOwnershipFlow(
    val artworkId: UniqueIdentifier,
    val partyToTransferTo: Party,
    val validityInMinutes: Long = 10
) : FlowLogic<WireTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): WireTransaction {

        val artworkStates = serviceHub.vaultService.queryBy(ArtworkState::class.java)
        val artworkStateAndRef =
            requireNotNull(artworkStates.states.singleOrNull { it.state.data.linearId == artworkId }) {
                "Unable to find an artwork state by the id: $artworkId"
            }
        val artworkState = artworkStateAndRef.state.data
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val wireTx = with(TransactionBuilder(notary)) {
            addInputState(artworkStateAndRef)
            addOutputState(artworkState.transferOwnershipTo(partyToTransferTo), ArtworkContract.ARTWORK_CONTRACT_ID)
            addCommand(ArtworkContract.Commands.TransferOwnership(), ourIdentity.owningKey, partyToTransferTo.owningKey)
            setTimeWindow(TimeWindow.untilOnly(Instant.now().plus(Duration.ofMinutes(validityInMinutes))))
        }.also { it.verify(serviceHub) }.toWireTransaction(serviceHub)

        val session = initiateFlow(partyToTransferTo)
        session.send(wireTx)

        val txDependencies = wireTx.getDependencies()
        txDependencies.forEach {
            val validatedTxDependency = serviceHub.validatedTransactions.getTransaction(it)
            if (validatedTxDependency == null) {
                FlowException("Unable to find validated transaction for input: $it")
            }
            subFlow(SendTransactionFlow(session, validatedTxDependency!!))
        }

        val txOk = session.receive<Boolean>().unwrap { it }
        if (!txOk) {
            FlowException("$partyToTransferTo Failed to validate draft transfer of ownership for tx id: ${wireTx.id}")
        }

        return wireTx
    }
}

@InitiatedBy(SendDraftTransferOfOwnershipFlow::class)
class SendDraftTransferOfOwnershipFlowHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Unit {

        val wireTx = otherSession.receive<WireTransaction>().unwrap { it }
        val txMerkleTree = wireTx.generateWireTransactionMerkleTree()
        val txOk = receiveAndVerifyTxDependencies(wireTx) && verifyShareConditions(
            wireTx,
            txMerkleTree
        ) && verifySharedTx(wireTx) //&& persistTxDetails(wireTx)

        otherSession.send(txOk)
    }

    @Suspendable
    private fun receiveAndVerifyTxDependencies(wireTransaction: WireTransaction): Boolean {
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

    @Suspendable
    private fun verifyShareConditions(wireTransaction: WireTransaction, expectedMerkleTree: MerkleTree): Boolean {
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

    @Suspendable
    private fun verifySharedTx(wireTransaction: WireTransaction): Boolean {
        val ledgerTx = wireTransaction.toLedgerTransaction(serviceHub)
        return try {
            ledgerTx.verify()
            true
        } catch (e: Exception) {
            logger.warn("Failed to resolve transaction: ${e.message}")
            false
        }
    }
}