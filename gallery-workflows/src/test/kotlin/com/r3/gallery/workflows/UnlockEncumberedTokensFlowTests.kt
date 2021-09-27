package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.utils.CBDC
import com.r3.gallery.utils.getNotaryTransactionSignature
import com.r3.gallery.workflows.internal.issueArtwork
import com.r3.gallery.workflows.internal.mockNetwork
import com.r3.gallery.workflows.internal.queryArtworkState
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
import org.junit.Test

class UnlockEncumberedTokensFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var seller: StartedMockNode
    private lateinit var gallery: StartedMockNode
    private lateinit var bidder: StartedMockNode
    private lateinit var buyer: StartedMockNode
    private lateinit var otherBidder: StartedMockNode
    private lateinit var otherBuyer: StartedMockNode

    @Before
    fun setup() {
        network = mockNetwork()
        seller = network.createPartyNode()
        gallery = network.createPartyNode()
        bidder = network.createPartyNode()
        buyer = network.createPartyNode()
        otherBidder = network.createPartyNode()
        otherBuyer = network.createPartyNode()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `can unlock encumbered GBP tokens from a swap between four parties`() {

        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkState = gallery.issueArtwork()
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty))

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).getOrThrow()

        val signedArtTransferTx = gallery.startFlow(SignAndFinalizeTransferOfOwnership(verifiedDraftTx.tx)).getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        seller.startFlow(UnlockEncumberedTokensFlow(signedTokensOfferTx.id, requiredSignature)).getOrThrow()

        val ownedByGallery = gallery.queryArtworkState(artworkState.artworkId, false)
        val ownedByBidder = bidder.queryArtworkState(artworkState.artworkId, false)
        val sellerBalance = seller.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val buyerBalance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assertNull(ownedByGallery)
        assertNotNull(ownedByBidder)
        assertEquals(10.GBP, sellerBalance)
        assertEquals(10.GBP, buyerBalance)
    }

    @Test
    fun `can unlock encumbered CBDC tokens from a swap between four parties`() {

        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkState = gallery.issueArtwork()
        buyer.startFlow(IssueTokensFlow(20.CBDC, buyerParty))

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.CBDC)).getOrThrow()

        val signedArtTransferTx = gallery.startFlow(SignAndFinalizeTransferOfOwnership(verifiedDraftTx.tx)).getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        seller.startFlow(UnlockEncumberedTokensFlow(signedTokensOfferTx.id, requiredSignature)).getOrThrow()

        val ownedByGallery = gallery.queryArtworkState(artworkState.artworkId, false)
        val ownedByBidder = bidder.queryArtworkState(artworkState.artworkId, false)
        val sellerBalance = seller.startFlow(GetBalanceFlow(CBDC)).getOrThrow()
        val buyerBalance = buyer.startFlow(GetBalanceFlow(CBDC)).getOrThrow()

        assertNull(ownedByGallery)
        assertNotNull(ownedByBidder)
        assertEquals(10.CBDC, sellerBalance)
        assertEquals(10.CBDC, buyerBalance)
    }
}

