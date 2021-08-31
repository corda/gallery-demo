package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.states.ArtworkState
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
class BuildDraftTransferOfOwnership(
    val artworkId: UniqueIdentifier,
    val partyToTransferTo: Party,
    val validityInMinutes: Long = 10
) : FlowLogic<WireTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): WireTransaction {
        val artworkStates = serviceHub.vaultService.queryBy(ArtworkState::class.java)
        val artworkStateAndRef =
            requireNotNull(artworkStates.states.singleOrNull { it.state.data.linearId == artworkId })
        val artworkState = artworkStateAndRef.state.data

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val txBuilder = with(TransactionBuilder(notary)) {
            addInputState(artworkStateAndRef)
            addOutputState(artworkState.transferOwnershipTo(partyToTransferTo), ArtworkContract.ARTWORK_CONTRACT_ID)
            addCommand(ArtworkContract.Commands.TransferOwnership(), ourIdentity.owningKey, partyToTransferTo.owningKey)
            setTimeWindow(TimeWindow.untilOnly(Instant.now().plus(Duration.ofMinutes(validityInMinutes))))
        }

        txBuilder.verify(serviceHub)
        val wtx = txBuilder.toWireTransaction(serviceHub)
        serviceHub.cacheService().cacheWireTransaction(wtx, this.ourIdentity)
        return wtx
    }
}

