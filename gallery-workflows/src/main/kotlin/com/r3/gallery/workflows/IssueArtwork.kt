package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Companion.ARTWORK_CONTRACT_ID
import com.r3.gallery.other.ArtworkOwnership
import com.r3.gallery.states.ArtworkOwnership
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IssueArtwork(val description: String, val url: String = "https://upload.wikimedia.org/wikipedia/en/e/e5/Magritte_TheSonOfMan.jpg") : FlowLogic<ArtworkState>() {

    @Suspendable
    override fun call(): ArtworkState {
        val partyA_CN1 = ourIdentity

        val state = ArtworkState(description, url, partyA_CN1, true)
        val command = Command(ArtworkContract.Commands.Issue(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
                .withItems(StateAndContract(state, ARTWORK_CONTRACT_ID), command)

        builder.verify(serviceHub)

        var stx = serviceHub.signInitialTransaction(builder)
        stx = subFlow(FinalityFlow(stx, emptyList()))

        val artworkState = stx.tx.outRefsOfType(ArtworkState::class.java).single()

        val b = artworkState.state.data == state

        return artworkState.state.data
    }
}
