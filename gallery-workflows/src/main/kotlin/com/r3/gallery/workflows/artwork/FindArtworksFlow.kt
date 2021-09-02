package com.r3.gallery.workflows.artwork

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.states.ArtworkState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy

@StartableByRPC
@InitiatingFlow
class FindArtworksFlow() :
    FlowLogic<List<ArtworkState>>() {

    @Suspendable
    override fun call(): List<ArtworkState> {
        return serviceHub
            .vaultService
            .queryBy<ArtworkState>()
            .states
            .map { it.state.data }
    }
}