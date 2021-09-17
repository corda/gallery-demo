package com.r3.gallery.workflows.artwork

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.states.ArtworkState
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@StartableByRPC
class DestroyArtwork(private val artworkId: ArtworkId?) : FlowLogic<Unit>() {

    constructor() : this(null)

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DestroyArtwork::class.java)
    }

    @Suspendable
    override fun call() {
        var ownedArtworks = serviceHub.vaultService.queryBy(ArtworkState::class.java).states
        artworkId?.let { ownedArtworks = ownedArtworks.filter { it.state.data.artworkId ==  artworkId } }
        if (ownedArtworks.isEmpty()) {
            logger.info("No owned artwork to destroy")
            return
        }

        val notary = ownedArtworks.first().state.notary

        val txBuilder = TransactionBuilder(notary)
            .addCommand(ArtworkContract.Commands.Destroy(), ourIdentity.owningKey)

        ownedArtworks.forEach {
            txBuilder.addInputState(it)
        }

        txBuilder.verify(serviceHub)
        val stx = serviceHub.signInitialTransaction(txBuilder)

        subFlow(FinalityFlow(stx, emptyList())).also {
            logger.info("Destroyed the following pieces ${ownedArtworks.map { it.state.data.artworkId }}")
        }
    }
}