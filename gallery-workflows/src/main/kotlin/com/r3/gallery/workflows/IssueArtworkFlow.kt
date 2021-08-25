package com.r3.gallery.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Companion.ARTWORK_CONTRACT_ID
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IssueArtworkFlow(val description: String, val url: String = "https://upload.wikimedia.org/wikipedia/en/e/e5/Magritte_TheSonOfMan.jpg") : FlowLogic<UniqueIdentifier>() {

    @Suspendable
    override fun call(): UniqueIdentifier {
        // REVIEW: ourIdentity = Party A / CN1
        val state = ArtworkState(description, url, ourIdentity, true)
        val command = Command(ArtworkContract.Commands.Issue(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
                .withItems(StateAndContract(state, ARTWORK_CONTRACT_ID), command)

        builder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(builder)
        subFlow(FinalityFlow(stx, emptyList())).tx.outputsOfType(ArtworkState::class.java).single()
        return state.linearId
    }
}
