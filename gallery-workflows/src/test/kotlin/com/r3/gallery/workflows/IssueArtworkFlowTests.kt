package com.r3.gallery.workflows

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import com.r3.gallery.workflows.internal.mockNetwork
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class IssueArtworkFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var gallery: StartedMockNode

    @Before
    fun setup() {
        network = mockNetwork()
        gallery = network.createPartyNode()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `can issue artwork to self`() {
        val now = Instant.now()
        val artworkId = ArtworkId.randomUUID()
        val flow = IssueArtworkFlow(artworkId, now.plusSeconds(3600), "test", url = "test-url")

        val artworkState: ArtworkState = gallery.startFlow(flow).getOrThrow()

        assertEquals(artworkId, artworkState.artworkId)
        assertEquals(gallery.info.chooseIdentity(), artworkState.owner)
        assertEquals(now.plusSeconds(3600), artworkState.expiry)
        assertEquals("test", artworkState.description)
        assertEquals("test-url", artworkState.url)
    }
}

