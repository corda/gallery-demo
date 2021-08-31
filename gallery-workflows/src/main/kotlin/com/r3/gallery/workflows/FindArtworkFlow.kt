package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

@StartableByRPC
@InitiatingFlow
class FindArtworkFlow private constructor(private val criteria: QueryCriteria) :
    FlowLogic<StateAndRef<ArtworkState>>() {

    constructor(linearId: UniqueIdentifier) : this(
        QueryCriteria.LinearStateQueryCriteria(
            linearId = listOf(linearId),
            contractStateTypes = setOf(ArtworkState::class.java)
        )
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