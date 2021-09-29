package com.r3.gallery.workflows

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.workflows.artwork.FindOwnedArtworksFlow
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import com.r3.gallery.workflows.internal.mockNetwork
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FindOwnedArtworksFlowTests {
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
    fun `can find issued artworks owned by self`() {
        val artworkId = ArtworkId.randomUUID()
        val artworkState = gallery.startFlow(
            IssueArtworkFlow(
                artworkId,
                Instant.now().plusSeconds(3600),
                "test",
                url = "test-url"
            )
        ).getOrThrow()

        val found = gallery.startFlow(FindOwnedArtworksFlow()).getOrThrow().single {
            it.state.data.artworkId == artworkId
        }.state.data

        Assert.assertEquals(artworkState.artworkId, found.artworkId)
        Assert.assertEquals(artworkState.owner, found.owner)
        Assert.assertEquals(artworkState.expiry, found.expiry)
        Assert.assertEquals(artworkState.description, found.description)
        Assert.assertEquals(artworkState.url, found.url)
    }
}

