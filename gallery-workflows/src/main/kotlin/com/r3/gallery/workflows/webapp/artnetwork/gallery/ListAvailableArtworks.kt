package com.r3.gallery.workflows.webapp.artnetwork.gallery

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.states.ArtworkState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker

/**
 * Lists artwork on the Art Network available for the gallery
 *
 * @param galleryParty
 */
@StartableByRPC
class ListAvailableArtworks(private val galleryParty: ArtworkParty) : FlowLogic<List<ArtworkId>>() {

    companion object {
        object QUERYING_ARTWORK : ProgressTracker.Step("Querying Artwork available.")

        fun tracker() = ProgressTracker(
            QUERYING_ARTWORK
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): List<ArtworkId> {
        progressTracker.currentStep = QUERYING_ARTWORK
        return serviceHub.vaultService.queryBy(ArtworkState::class.java)
            .states.map { it.state.data.artworkId }.distinct()
    }
}