package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

@StartableByRPC
@InitiatingFlow
class FindArtworksFlow private constructor(private val criteria: QueryCriteria) :
    FlowLogic<List<StateAndRef<ArtworkState>>>() {

    constructor() : this(
        QueryCriteria.LinearStateQueryCriteria(
            status = Vault.StateStatus.UNCONSUMED,
            contractStateTypes = setOf(ArtworkState::class.java)
        )
    )

    @Suspendable
    override fun call(): List<StateAndRef<ArtworkState>> {
        return serviceHub
            .vaultService
            .queryBy<ArtworkState>(criteria)
            .states
    }
}