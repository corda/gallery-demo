//package com.r3.gallery.workflows.webapp.artnetwork
//
//import com.r3.gallery.api.ArtworkId
//import com.r3.gallery.api.ArtworkOwnership
//import com.r3.gallery.workflows.artwork.IssueArtworkFlow
//import com.r3.gallery.workflows.webapp.artnetwork.gallery.IssueArtworkFlow
//import com.r3.gallery.workflows.webapp.artnetwork.gallery.ListAvailableArtworks
//import com.r3.gallery.workflows.webapp.artnetwork.gallery.utilityflows.ArtworkIdToState
//import com.r3.gallery.workflows.webapp.exceptions.InvalidArtworkIdException
//import net.corda.core.contracts.requireThat
//import net.corda.core.utilities.getOrThrow
//import net.corda.testing.node.MockNetwork
//import net.corda.testing.node.MockNetworkParameters
//import net.corda.testing.node.StartedMockNode
//import net.corda.testing.node.TestCordapp
//import org.junit.jupiter.api.AfterAll
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//import java.lang.IllegalArgumentException
//import java.util.*
//
//class TestGalleryFlows {
//
//    companion object {
//        private var network: MockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
//            TestCordapp.findCordapp("com.r3.gallery.contracts"),
//            TestCordapp.findCordapp("com.r3.gallery.workflows")
//        )))
//
//        @AfterAll
//        fun tearDown() {
//            this.network.stopNodes()
//        }
//    }
//
//    private var gallery: StartedMockNode = network.createPartyNode()
//    private var bidder: StartedMockNode = network.createPartyNode()
//
//    private var testArtworkId: ArtworkId = UUID.randomUUID()
//    private var testArtworkOwnership: ArtworkOwnership
//
//    init {
//        // create an initial issuance
//        val issueFlow = IssueArtworkFlow(testArtworkId)
//        testArtworkOwnership = gallery.startFlow(issueFlow).getOrThrow()
//        network.runNetwork()
//    }
//
//    @Test
//    fun `artwork id to state conversion`() {
//        // run flow and check correctness of result - art has been issued in @Before block
//        gallery.startFlow(
//            ArtworkIdToState(testArtworkId)
//        ).getOrThrow()
//            .also {
//                requireThat {
//                    "artworkId must be as expected" using (testArtworkId == it.artworkId)
//                    "cordaReference must be as expected" using (testArtworkOwnership.cordaReference == it.linearId.id)
//                }
//            }
//
//        // attempt to fetch with invalid ID
//        assertThrows<InvalidArtworkIdException> {
//            gallery.startFlow(
//                ArtworkIdToState(UUID.randomUUID()) // unregistered ID
//            ).getOrThrow()
//        }
//    }
//
//    @Test
//    fun `issue artwork flow correctly checks if artwork already exists`() {
//        val issueDupeId = IssueArtworkFlow(testArtworkId)
//        assertThrows<IllegalArgumentException> {
//            gallery.startFlow(issueDupeId).getOrThrow()
//        }.also {
//            require(it.message?.contains("already exist")!!)
//        }
//    }
//
//    @Test
//    fun `list available artwork returns correctly`() {
//        // issue three additional artworks
//        (1..3).forEach {
//            gallery.startFlow(IssueArtworkFlow(UUID.randomUUID())).getOrThrow()
//        }
//
//        val galleryParty = gallery.info.legalIdentities.first().name.toString()
//        gallery.startFlow(ListAvailableArtworks(galleryParty = galleryParty))
//            .getOrThrow()
//            .also {
//                requireThat {
//                    "There must be 4 pieces of art in the list of the gallery party" using (it.size == 4)
//                    "Contains a target expected artworkId" using (it.contains(testArtworkId))
//                }
//            }
//    }
//
//}