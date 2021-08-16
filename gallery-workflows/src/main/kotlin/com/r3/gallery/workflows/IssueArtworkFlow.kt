package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Companion.ARTWORK_CONTRACT_ID
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IssueArtworkInitiator(val description: String, val url: String = "https://upload.wikimedia.org/wikipedia/en/e/e5/Magritte_TheSonOfMan.jpg") : FlowLogic<UniqueIdentifier>() {

    @Suspendable
    override fun call(): UniqueIdentifier {
        val state = ArtworkState(description, url, ourIdentity)
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