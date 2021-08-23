//package com.r3.gallery.workflows.webapp.artnetwork.gallery
//
//import co.paralleluniverse.fibers.Suspendable
//import com.r3.gallery.api.ArtworkId
//import com.r3.gallery.api.ArtworkOwnership
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.StartableByRPC
//import net.corda.core.utilities.ProgressTracker
//
//@StartableByRPC
//class GetOwnership(private val artworkId: ArtworkId) : FlowLogic<ArtworkOwnership>() {
//
//    companion object {
//        object QUERYING_ARTWORK_OWNERSHIP : ProgressTracker.Step("Querying artwork ownership.")
//
//        fun tracker() = ProgressTracker(
//            QUERYING_ARTWORK_OWNERSHIP
//        )
//    }
//
//    override val progressTracker = tracker()
//
//    @Suspendable
//    override fun call(): ArtworkOwnership {
//        progressTracker.currentStep = QUERYING_ARTWORK_OWNERSHIP
//        return
//    }
//}