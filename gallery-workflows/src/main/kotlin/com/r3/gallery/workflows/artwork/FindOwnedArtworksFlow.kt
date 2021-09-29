package com.r3.gallery.workflows.artwork

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy

/**
 * Find all artwork states for the local node owned by the node itself.
 * @return the [List] of [ArtworkState]'s [StateAndRef]s.
 */
@StartableByRPC
@InitiatingFlow
class FindOwnedArtworksFlow :
    FlowLogic<List<StateAndRef<ArtworkState>>>() {

    @Suspendable
    override fun call(): List<StateAndRef<ArtworkState>> {
        return serviceHub.vaultService.queryBy<ArtworkState>().states.filter { it.state.data.owner == ourIdentity }
    }
}