package com.r3.gallery.workflows.artwork

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.states.ArtworkState
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy

/**
 * Finds an artwork state based on its ID
 * @param artworkId the UUID of the artwork (not the state linear id)
 */
@StartableByRPC
@InitiatingFlow
class FindArtworkFlow(private val artworkId: ArtworkId) : FlowLogic<ArtworkState>() {

    @Suspendable
    override fun call(): ArtworkState {
        return serviceHub
            .vaultService
            .queryBy<ArtworkState>()
            .states
            .singleOrNull { it.state.data.artworkId == artworkId }?.state?.data
            ?: throw FlowException("Failed to find state.")
    }
}