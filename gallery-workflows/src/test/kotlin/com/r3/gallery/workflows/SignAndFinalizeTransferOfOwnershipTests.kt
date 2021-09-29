package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.workflows.internal.issueArtwork
import com.r3.gallery.workflows.internal.mockNetwork
import com.r3.gallery.workflows.token.GetBalanceFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class SignAndFinalizeTransferOfOwnershipTests {
    private lateinit var network: MockNetwork
    private lateinit var gallery: StartedMockNode
    private lateinit var bidder: StartedMockNode

    @Before
    fun setup() {
        network = mockNetwork()
        gallery = network.createPartyNode()
        bidder = network.createPartyNode()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `gallery can sign bidder-validated unsigned tx`() {
        val galleryParty = gallery.info.chooseIdentity()
        val artworkState = gallery.issueArtwork()
        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())

        val unsignedTx = bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()

        gallery.startFlow(SignAndFinalizeTransferOfOwnership(unsignedTx.tx)).getOrThrow()
    }
}

