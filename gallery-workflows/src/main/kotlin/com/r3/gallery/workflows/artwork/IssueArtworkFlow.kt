package com.r3.gallery.workflows.artwork

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkOwnership
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Companion.ID
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.internal.toX500Name
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IssueArtworkFlow(
    private val artworkId: ArtworkId
) : FlowLogic<ArtworkOwnership>() {

    @Suspendable
    override fun call(): ArtworkOwnership {
        // REVIEW: ourIdentity = Party A / CN1
        val state = ArtworkState(artworkId, ourIdentity, ourIdentity)
        val command = Command(ArtworkContract.Commands.Issue(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            .withItems(StateAndContract(state, ID), command)

        builder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(builder)
        subFlow(FinalityFlow(stx, emptyList())).tx.outputsOfType(ArtworkState::class.java).single()

        return ArtworkOwnership(
            state.linearId.id,
            state.artworkId,
            state.owner.nameOrNull()!!.toX500Name().toString()
        )
    }
}
