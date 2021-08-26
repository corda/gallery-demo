package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import java.time.Duration
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class BuildDraftTransferOfOwnership(val artworkId: UniqueIdentifier, val bidder: Party) : FlowLogic<WireTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): WireTransaction {
        val auctionStates = serviceHub.vaultService.queryBy(ArtworkState::class.java)
        val inputStateAndRef = requireNotNull(auctionStates.states.find { it.state.data.linearId == artworkId })
        val inputState = inputStateAndRef.state.data

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val transactionBuilder = TransactionBuilder(notary = notary)
                .addInputState(inputStateAndRef)
                .addOutputState(inputState.awardTo(bidder), ArtworkContract.ARTWORK_CONTRACT_ID)
                .addCommand(ArtworkContract.Commands.TransferOwnership(), ourIdentity.owningKey, bidder.owningKey)
                .setTimeWindow(TimeWindow.untilOnly(Instant.now().plus(Duration.ofMinutes(5))))

        transactionBuilder.verify(serviceHub)

        val tx = transactionBuilder.toWireTransaction(serviceHub)

        // TODO: this is for testing purpose
        serviceHub.cacheService().cacheWireTransaction(tx, this.ourIdentity)

        return tx
    }
}

