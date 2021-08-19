package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Companion.ARTWORK_CONTRACT_ID
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.*
import net.corda.core.crypto.MerkleTree
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.workflows.*
import java.time.Duration
import java.time.Instant
import java.util.*

@StartableByRPC
@InitiatingFlow
class IssueArtworkFlow(val description: String, val url: String = "https://upload.wikimedia.org/wikipedia/en/e/e5/Magritte_TheSonOfMan.jpg") : FlowLogic<UniqueIdentifier>() {

    @Suspendable
    override fun call(): UniqueIdentifier {
        val state = ArtworkState(description, url, ourIdentity, true)
        val command = Command(ArtworkContract.Commands.Issue(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
                .withItems(StateAndContract(state, ARTWORK_CONTRACT_ID), command)

        builder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(builder)
        subFlow(FinalityFlow(stx, emptyList())).tx.outputsOfType(ArtworkState::class.java).single()
        return state.linearId
    }
}

@StartableByRPC
@InitiatingFlow
class FindArtworkFlow private constructor(private val criteria: QueryCriteria) : FlowLogic<StateAndRef<ArtworkState>>() {

    constructor(linearId: UniqueIdentifier) : this(
        QueryCriteria.LinearStateQueryCriteria(
                linearId = listOf(linearId),
                contractStateTypes = setOf(ArtworkState::class.java))
    )

    @Suspendable
    override fun call(): StateAndRef<ArtworkState> {
        return serviceHub
                .vaultService
                .queryBy<ArtworkState>(criteria)
                .states
                .singleOrNull()
                ?: throw FlowException("Failed to find state.")
    }
}

@StartableByRPC
@InitiatingFlow
class FindArtworksFlow private constructor(private val criteria: QueryCriteria) : FlowLogic<List<StateAndRef<ArtworkState>>>() {

    constructor() : this(
            QueryCriteria.LinearStateQueryCriteria(
                    status = Vault.StateStatus.UNCONSUMED,
                    contractStateTypes = setOf(ArtworkState::class.java))
    )

    @Suspendable
    override fun call(): List<StateAndRef<ArtworkState>> {
        return serviceHub
                .vaultService
                .queryBy<ArtworkState>(criteria)
                .states
    }
}

@InitiatingFlow
@StartableByRPC
class SelfIssueCashFlow(val amount: Amount<Currency>) : FlowLogic<Cash.State>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): Cash.State {

        val issueRef = OpaqueBytes.of(0)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cashIssueTransaction = subFlow(CashIssueFlow(amount, issueRef, notary))
        return cashIssueTransaction.stx.tx.outputs.single().data as Cash.State
    }
}

@InitiatingFlow
@StartableByRPC
class GetCashBalanceFlow() : FlowLogic<Map<Currency, Amount<Currency>>>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): Map<Currency, Amount<Currency>> {
        return serviceHub.getCashBalances()
    }
}

@InitiatingFlow
@StartableByRPC
class GetDraftTransferOfOwnership(val artworkId: UniqueIdentifier, val bidder: Party) : FlowLogic<WireTransaction>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call(): WireTransaction {
        val auctionStates = serviceHub.vaultService.queryBy(ArtworkState::class.java)
        val inputStateAndRef = requireNotNull(auctionStates.states.find { it.state.data.linearId == artworkId })
        val inputState = inputStateAndRef.state.data

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val transactionBuilder = TransactionBuilder(notary = notary)
                .addCommand(Command(ArtworkContract.Commands.TransferOwnership(), listOf(ourIdentity.owningKey, bidder.owningKey)))
                .addInputState(inputStateAndRef)
                .addOutputState(inputState.awardTo(bidder), ARTWORK_CONTRACT_ID)
                .setTimeWindow(TimeWindow.untilOnly(Instant.now().plus(Duration.ofMinutes(5))))

        transactionBuilder.verify(serviceHub)

        return transactionBuilder.toWireTransaction(serviceHub)
    }
}

@InitiatingFlow
@StartableByRPC
class ShareDraftTransferOfOwnershipFlow(val artworkId: UniqueIdentifier, val bidder: Party) : FlowLogic<Boolean>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Boolean {
        val session = initiateFlow(bidder)
        val draftTx = subFlow(GetDraftTransferOfOwnership(artworkId, bidder))
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
                verifySharedTx(draftTx) &&
                true//persistTxDetails(draftTx)

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
