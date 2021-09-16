package com.r3.gallery.workflows.artwork

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Companion.ID
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IssueArtworkFlow(
    private val artworkId: ArtworkId,
    private val description: String = "",
    private val url: String = ""
) : FlowLogic<ArtworkState>() {

    @Suspendable
    override fun call(): ArtworkState {
        val state = ArtworkState(artworkId, ourIdentity, description, url)
        val command = Command(ArtworkContract.Commands.Issue(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            .withItems(StateAndContract(state, ID), command)

        builder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(builder)
        return subFlow(FinalityFlow(stx, emptyList())).tx.outputsOfType(ArtworkState::class.java).single()
    }
}
