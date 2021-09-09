//package com.r3.gallery.workflows.webapp.artnetwork.gallery.utilityflows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.r3.gallery.api.ArtworkId
//import com.r3.gallery.states.ArtworkState
//import com.r3.gallery.workflows.webapp.artworkIdToState
//import com.r3.gallery.workflows.webapp.exceptions.InvalidArtworkIdException
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.StartableByRPC
//
//@StartableByRPC
//class ArtworkIdToState(private val artworkId: ArtworkId) : FlowLogic<ArtworkState>() {
//
//    @Suspendable
//    override fun call(): ArtworkState {
//        serviceHub.artworkIdToState(artworkId)?.let {
//            return it
//        }
//        throw InvalidArtworkIdException(artworkId) // not found
//    }
//}