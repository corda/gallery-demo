package com.r3.gallery.workflows

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.workflows.artwork.FindArtworkFlow
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import com.r3.gallery.workflows.internal.mockNetwork
import net.corda.core.flows.FlowException
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.time.Instant
import kotlin.test.assertEquals

class FindArtworkFlowTests {
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
    fun `can find issued artwork owned by self`() {
        val artworkState = gallery.startFlow(
            IssueArtworkFlow(
                ArtworkId.randomUUID(),
                Instant.now().plusSeconds(3600),
                "test",
                url = "test-url"
            )
        ).getOrThrow()

        val found = gallery.startFlow(FindArtworkFlow(artworkState.artworkId)).getOrThrow()

        Assert.assertEquals(artworkState.artworkId, found.artworkId)
        Assert.assertEquals(artworkState.owner, found.owner)
        Assert.assertEquals(artworkState.expiry, found.expiry)
        Assert.assertEquals(artworkState.description, found.description)
        Assert.assertEquals(artworkState.url, found.url)
    }

    @Test
    fun `throws for non-existing artwork query`() {
        gallery.startFlow(
            IssueArtworkFlow(
                ArtworkId.randomUUID(),
                Instant.now().plusSeconds(3600),
                "test",
                url = "test-url"
            )
        ).getOrThrow()

        Assertions.assertThrows(FlowException::class.java) {
            gallery.startFlow(FindArtworkFlow(ArtworkId.randomUUID())).getOrThrow()
        }
    }
}

