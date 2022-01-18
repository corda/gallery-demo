package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.utils.getNotaryTransactionSignature
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import com.r3.gallery.workflows.internal.issueArtwork
import com.r3.gallery.workflows.internal.mockNetwork
import com.r3.gallery.workflows.internal.moveClock
import com.r3.gallery.workflows.internal.queryArtworkState
import com.r3.gallery.workflows.token.GetBalanceFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.time.Instant

class RevertEncumberedTokenFlowTests {
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
    @Ignore("Need to parametrize swap tx TimeWindow expiration or LockCommand.Release")
    fun `buyer can revert tokens after sale times out`() {

        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkState = gallery.issueArtwork()
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty))

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()

        val buyerInitialBalance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).getOrThrow()

        val buyerBalanceAfterOffer = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        buyer.startFlow(RevertEncumberedTokensFlow(signedTokensOfferTx.id)).getOrThrow()

        val buyerBalanceAfterRedeem = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assertTrue(buyerBalanceAfterOffer < buyerInitialBalance)
        assertTrue(buyerBalanceAfterRedeem == buyerInitialBalance)
    }

    @Test
    fun `seller can revert encumbered tokens back to a buyer`() {

        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkState = gallery.issueArtwork()
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty))

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()

        val buyerInitialBalance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).getOrThrow()

        val buyerBalanceAfterOffer = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        moveClock(setOf(gallery, seller, buyer, bidder, network.defaultNotaryNode), 6000)
        network.waitQuiescent()

        seller.startFlow(RevertEncumberedTokensFlow(signedTokensOfferTx.id)).getOrThrow()

        val buyerBalanceAfterRedeem = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assertTrue(buyerBalanceAfterOffer < buyerInitialBalance)
        assertTrue(buyerBalanceAfterRedeem == buyerInitialBalance)
    }

    @Test
    fun `seller can revert encumbered tokens back to multiple offering buyers`() {
        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()
        val otherBuyerParty = otherBuyer.info.chooseIdentity()

        val artworkState = gallery.issueArtwork()
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty))
        otherBuyer.startFlow(IssueTokensFlow(20.GBP, otherBuyerParty))

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())

        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()
        val otherVerifiedDraftTx =
            otherBidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()

        val buyerInitialBalance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val otherBuyerInitialBalance = otherBuyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).getOrThrow()
        val otherSignedTokensOfferTx =
            otherBuyer.startFlow(OfferEncumberedTokensFlow(sellerParty, otherVerifiedDraftTx, 10.GBP)).getOrThrow()

        val buyerBalanceAfterOffer = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val otherBuyerBalanceAfterOffer = otherBuyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        moveClock(setOf(gallery, seller, buyer, bidder, otherBuyer, otherBidder, network.defaultNotaryNode), 6000)
        network.waitQuiescent()

        seller.startFlow(RevertEncumberedTokensFlow(signedTokensOfferTx.id)).getOrThrow()
        seller.startFlow(RevertEncumberedTokensFlow(otherSignedTokensOfferTx.id)).getOrThrow()

        val buyerBalanceAfterRevert = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val otherBuyerBalanceAfterRevert = otherBuyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assertTrue(buyerBalanceAfterOffer < buyerInitialBalance)
        assertTrue(buyerBalanceAfterRevert == buyerInitialBalance)
        assertTrue(otherBuyerBalanceAfterOffer < otherBuyerInitialBalance)
        assertTrue(otherBuyerBalanceAfterRevert == otherBuyerInitialBalance)
    }

    @Test
    fun `seller can receive tokens from a buyer and revert tokens from another buyer`() {
        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()
        val otherBuyerParty = otherBuyer.info.chooseIdentity()

        val artworkState = gallery.issueArtwork()
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty))
        otherBuyer.startFlow(IssueTokensFlow(20.GBP, otherBuyerParty))

        // all balances before the auction starts
        val sellerInitialBalance = seller.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val buyerInitialBalance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val otherBuyerInitialBalance = otherBuyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())

        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()
        val otherVerifiedDraftTx =
            otherBidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).getOrThrow()
        val otherSignedTokensOfferTx =
            otherBuyer.startFlow(OfferEncumberedTokensFlow(sellerParty, otherVerifiedDraftTx, 10.GBP)).getOrThrow()

        // all balances after initial art offer
        val buyerBalanceAfterOffer = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val otherBuyerBalanceAfterOffer = otherBuyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val sellerBalanceAfterOffers = seller.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        // Accept one of the bids (bidder win, otherBidder lose)
        val signedArtTransferTx = gallery.startFlow(SignAndFinalizeTransferOfOwnership(verifiedDraftTx.tx)).getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        // claim winning bidder's tokens
        seller.startFlow(UnlockEncumberedTokensFlow(signedTokensOfferTx.id, requiredSignature)).getOrThrow()

        moveClock(setOf(gallery, seller, buyer, bidder, otherBuyer, otherBidder, network.defaultNotaryNode), 6000)
        network.waitQuiescent()
        // early-revert token to losing bidder
        seller.startFlow(RevertEncumberedTokensFlow(otherSignedTokensOfferTx.id)).getOrThrow()

        // get new balances and ownerships
        val sellerBalanceAfterUnlock = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val buyerBalanceAfterUnlock = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val otherBuyerBalanceAfterRevert = otherBuyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        val artworkItemGallery = gallery.queryArtworkState(artworkState.artworkId, false)
        val artworkItemBidder = bidder.queryArtworkState(artworkState.artworkId, false)
        val artworkItemOtherBidder = otherBidder.queryArtworkState(artworkState.artworkId, false)

        // artwork item now belongs to the bidder, buyer has been charged, other buyer refunded
        assertNotNull(artworkItemBidder)
        assertNull(artworkItemGallery)
        assertNull(artworkItemOtherBidder)
        assertTrue(sellerBalanceAfterOffers == sellerInitialBalance)
        assertTrue(sellerBalanceAfterUnlock > sellerInitialBalance)
        assertTrue(buyerBalanceAfterOffer < buyerInitialBalance)
        assertTrue(buyerBalanceAfterUnlock == buyerBalanceAfterOffer)
        assertTrue(otherBuyerBalanceAfterOffer < otherBuyerInitialBalance)
        assertTrue(otherBuyerBalanceAfterRevert == otherBuyerInitialBalance)
    }
}

