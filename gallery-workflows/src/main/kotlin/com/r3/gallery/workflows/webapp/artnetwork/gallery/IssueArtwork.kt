package com.r3.gallery.workflows.webapp.artnetwork.gallery

import co.paralleluniverse.fibers.Suspendable
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkOwnership
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Commands
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.webapp.artworkExists
import com.r3.gallery.workflows.webapp.firstNotary
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.internal.toX500Name
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Issues artwork on the Art Network
 *
 * @param artworkId unique webapp reference for binding to assets and metadata
 */
@StartableByRPC
class IssueArtworkFlow(private val artworkId: ArtworkId) : FlowLogic<ArtworkOwnership>() {
    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating a issue artwork transaction.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object FINALISING_TRANSACTION : ProgressTracker.Step("Recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): ArtworkOwnership {
        requireThat { "Artwork must not already exit on network" using serviceHub.artworkExists(artworkId) }

        val artState = ArtworkState(
            issuer = ourIdentity,
            owner = ourIdentity,
            artworkId = artworkId,
            participants = listOf(ourIdentity),
            linearId = UniqueIdentifier()
        )

        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = TransactionBuilder(firstNotary())
            .addOutputState(artState, ArtworkContract.ID)
            .addCommand(Commands.Issue(), ourIdentity.owningKey)

        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(txBuilder)

        serviceHub.recordTransactions(stx)

        val issuedState = stx.coreTransaction.outputsOfType(ArtworkState::class.java).first()

        return ArtworkOwnership(
            issuedState.linearId.id,
            issuedState.artworkId,
            issuedState.owner.nameOrNull()!!.toX500Name().toString()
        )
    }
}