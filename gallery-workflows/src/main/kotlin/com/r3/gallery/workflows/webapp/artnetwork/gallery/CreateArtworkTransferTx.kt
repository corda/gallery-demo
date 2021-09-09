//package com.r3.gallery.workflows.webapp.artnetwork.gallery
//
//import co.paralleluniverse.fibers.Suspendable
//import com.r3.gallery.api.*
//import com.r3.gallery.contracts.ArtworkContract
//import com.r3.gallery.workflows.webapp.artworkIdToState
//import com.r3.gallery.workflows.webapp.artworkPartyToParty
//import com.r3.gallery.workflows.webapp.exceptions.InvalidArtworkIdException
//import com.r3.gallery.workflows.webapp.firstNotary
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.StartableByRPC
//import net.corda.core.serialization.serialize
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.ProgressTracker
//
///**
// * Creates an unsigned transaction for transfer of artwork between gallery
// * and bidder.
// */
//@StartableByRPC
//class CreateArtworkTransferTx(
//    private val bidderParty: ArtworkParty,
//    private val artworkOwnership: ArtworkOwnership
//) : FlowLogic<UnsignedArtworkTransferTx>() {
//
//    companion object {
//        object FETCHING_ARTWORK : ProgressTracker.Step("Fetching Artwork from ownership.")
//        object CREATING_TRANSACTION : ProgressTracker.Step("Creating transaction for ownership transfer.")
//
//        fun tracker() = ProgressTracker(
//            FETCHING_ARTWORK,
//            CREATING_TRANSACTION
//        )
//    }
//
//    override val progressTracker = tracker()
//
//    @Suspendable
//    override fun call(): UnsignedArtworkTransferTx {
//        progressTracker.currentStep = FETCHING_ARTWORK
//        val artworkId = artworkOwnership.artworkId
//        val artworkState = serviceHub.artworkIdToState(artworkId) ?: throw InvalidArtworkIdException(artworkId)
//
//        val bidderCordaParty = serviceHub.artworkPartyToParty(bidderParty)
//        val artworkCommandStateNewOwner = artworkState.withNewOwner(bidderCordaParty)
//
//        progressTracker.currentStep = CREATING_TRANSACTION
//        val txBuilder = TransactionBuilder(firstNotary())
//            // requires our signature but will not attach to transaction
//            .addCommand(artworkCommandStateNewOwner.command, ourIdentity.owningKey)
//            .addOutputState(artworkCommandStateNewOwner.ownableState, ArtworkContract.ID)
//
//        val serializedTXArray = txBuilder.toWireTransaction(serviceHub).serialize().bytes
//        return UnsignedArtworkTransferTx(serializedTXArray)
//    }
//}