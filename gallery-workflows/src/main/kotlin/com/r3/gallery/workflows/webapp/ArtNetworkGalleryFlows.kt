package com.r3.gallery.workflows.webapp

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkOwnership
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.contracts.ArtworkContract
import com.r3.gallery.contracts.ArtworkContract.Commands
import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.internal.toX500Name
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Issues artwork on the Art Network
 *
 * @param galleryParty
 */
@StartableByRPC
class IssueArtworkFlow(private val galleryParty: ArtworkParty, private val artworkId: ArtworkId) : FlowLogic<ArtworkOwnership>() {
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

    override fun call(): ArtworkOwnership {
        val ownerGalleryParty = serviceHub.artworkPartyToParty(galleryParty)
        // todo: explicit notary selection
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        requireThat { "Artwork must not already exit on network" using serviceHub.artworkExists(artworkId) }

        val artState = ArtworkState(
            issuer = ourIdentity,
            owner = ownerGalleryParty,
            artworkId = artworkId,
            participants = listOf(ourIdentity, ownerGalleryParty),
            linearId = UniqueIdentifier()
        )

        progressTracker.currentStep = GENERATING_TRANSACTION
        val txBuilder = TransactionBuilder(notary)
            .addOutputState(artState, ArtworkContract.ID)
            .addCommand(Commands.Issue(), ourIdentity.owningKey)

        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        var stx = ptx

        val sessions = initiateFlowSessions(txBuilder)
        sessions?.let {
            stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        }

        return subFlow(FinalityFlow(stx, sessions ?: emptyList())).let {
            val issuedState = it.coreTransaction.outputsOfType(ArtworkState::class.java).first()
            ArtworkOwnership(
                issuedState.linearId.id,
                issuedState.artworkId,
                issuedState.owner.nameOrNull()!!.toX500Name().toString()
            )
        }
    }
}