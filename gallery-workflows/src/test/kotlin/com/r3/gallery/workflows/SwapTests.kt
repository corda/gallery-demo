package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.utils.AuctionCurrency
import com.r3.gallery.utils.CBDC
import com.r3.gallery.utils.getNotaryTransactionSignature
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import com.r3.gallery.workflows.token.GetBalanceFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
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
import java.util.concurrent.Future


class SwapTests {
    private lateinit var network: MockNetwork
    private lateinit var seller: StartedMockNode
    private lateinit var gallery: StartedMockNode
    private lateinit var bidder: StartedMockNode
    private lateinit var buyer: StartedMockNode
    private lateinit var otherBidder: StartedMockNode
    private lateinit var otherBuyer: StartedMockNode

    @Before
    fun setup() {
        val notaries = listOf(
            MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB")),
            MockNetworkNotarySpec(CordaX500Name("Notary", "Zurich", "CH"))
        )

        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.r3.gallery.contracts"),
                    TestCordapp.findCordapp("com.r3.gallery.workflows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
                )
            ).withNotarySpecs(notaries)
        )
        seller = network.createPartyNode()
        gallery = network.createPartyNode()
        bidder = network.createPartyNode()
        buyer = network.createPartyNode()
        otherBidder = network.createPartyNode()
        otherBuyer = network.createPartyNode()

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `can issue artwork`() {
        //val flow = IssueArtworkFlow(description = "test artwork $epoch", url = "http://www.google.com/search?q=$epoch")
        val flow = IssueArtworkFlow(ArtworkId.randomUUID(), Instant.now().plusSeconds(5000L))
        seller.startFlow(flow).also { network.runNetwork() }.getOrThrow()
        // TODO
    }

    @Test
    fun `swap steps between four parties with GBP`() {

        val galleryParty = gallery.info.chooseIdentity()
        //val bidderParty = bidder.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkState = issueArtwork(gallery)
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty)).apply {
            network.runNetwork()
        }

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                network.runNetwork()
            }.getOrThrow().second

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).apply {
                network.runNetwork()
            }.getOrThrow()

        val signedArtTransferTx = gallery.startFlow(SignAndFinalizeTransferOfOwnership(verifiedDraftTx.tx)).apply {
            network.runNetwork()
        }.getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        seller.startFlow(UnlockEncumberedTokensFlow(signedTokensOfferTx.id, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        val artworkItemGallery = queryArtworkState(gallery, false)
        assertNull(artworkItemGallery)

        val artworkItemBidder = queryArtworkState(bidder, false)
        assertNotNull(artworkItemBidder)

//        val sellerBalance = seller.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
//        val buyerBalance = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
    }

    @Test
    fun `swap steps between four parties with CBDC`() {

        val galleryParty = gallery.info.chooseIdentity()
        //val bidderParty = bidder.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkState = issueArtwork(gallery)
        buyer.startFlow(IssueTokensFlow(20.CBDC, buyerParty)).apply {
            network.runNetwork()
        }

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                network.runNetwork()
            }.getOrThrow().second

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.CBDC)).apply {
                network.runNetwork()
            }.getOrThrow()

        val signedArtTransferTx = gallery.startFlow(SignAndFinalizeTransferOfOwnership(verifiedDraftTx.tx)).apply {
            network.runNetwork()
        }.getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        seller.startFlow(UnlockEncumberedTokensFlow(signedTokensOfferTx.id, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        val artworkItemGallery = queryArtworkState(gallery, false)
        assertNull(artworkItemGallery)

        val artworkItemBidder = queryArtworkState(bidder, false)
        assertNotNull(artworkItemBidder)

//        val sellerBalance = seller.startFlow(GetBalanceFlow(CBDC)).also { network.runNetwork() }.getOrThrow()
//        val buyerBalance = buyer.startFlow(GetBalanceFlow(CBDC)).also { network.runNetwork() }.getOrThrow()
    }

    @Test
    @Ignore("Need to parametrize swap tx TimeWindow expiration or LockCommand.Release")
    fun `revert steps between four parties by encumbered tx issuer`() {

        val galleryParty = gallery.info.chooseIdentity()
        //val bidderParty = bidder.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkState = issueArtwork(gallery)
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty)).apply {
            network.runNetwork()
        }

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                network.runNetwork()
            }.getOrThrow().second

        val buyerInitialBalance = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).apply {
                network.runNetwork()
            }.getOrThrow()

        val buyerBalanceAfterOffer = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        buyer.startFlow(RevertEncumberedTokensFlow(signedTokensOfferTx.id)).apply {
            network.runNetwork()
        }.getOrThrow()

        val buyerBalanceAfterRedeem = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        assertTrue(buyerBalanceAfterOffer < buyerInitialBalance)
        assertTrue(buyerBalanceAfterRedeem == buyerInitialBalance)
    }

    @Test
    fun `revert steps between four parties by encumbered tx receiver`() {

        val galleryParty = gallery.info.chooseIdentity()
        //val bidderParty = bidder.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkState = issueArtwork(gallery)
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty)).apply {
            network.runNetwork()
        }

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                network.runNetwork()
            }.getOrThrow().second

        val buyerInitialBalance = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).apply {
                network.runNetwork()
            }.getOrThrow()

        val buyerBalanceAfterOffer = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        seller.startFlow(RevertEncumberedTokensFlow(signedTokensOfferTx.id)).apply {
            network.runNetwork()
        }.getOrThrow()

        val buyerBalanceAfterRedeem = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        assertTrue(buyerBalanceAfterOffer < buyerInitialBalance)
        assertTrue(buyerBalanceAfterRedeem == buyerInitialBalance)
    }

    @Test
    fun `revert steps between four parties by encumbered tx receiver with multiple bids`() {
        val galleryParty = gallery.info.chooseIdentity()
        //val bidderParty = bidder.info.chooseIdentity()
        //val otherBidderParty = otherBidder.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()
        val otherBuyerParty = otherBuyer.info.chooseIdentity()

        val artworkState = issueArtwork(gallery)
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty)).apply { network.runNetwork() }
        otherBuyer.startFlow(IssueTokensFlow(20.GBP, otherBuyerParty)).apply { network.runNetwork() }

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())

        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                network.runNetwork()
            }.getOrThrow().second
        val otherVerifiedDraftTx =
            otherBidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                network.runNetwork()
            }.getOrThrow().second

        val buyerInitialBalance = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val otherBuyerInitialBalance = otherBuyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).apply {
                network.runNetwork()
            }.getOrThrow()
        val otherSignedTokensOfferTx =
            otherBuyer.startFlow(OfferEncumberedTokensFlow(sellerParty, otherVerifiedDraftTx, 10.GBP)).apply {
                network.runNetwork()
            }.getOrThrow()

        val buyerBalanceAfterOffer = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val otherBuyerBalanceAfterOffer = otherBuyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        seller.startFlow(RevertEncumberedTokensFlow(signedTokensOfferTx.id)).apply {
            network.runNetwork()
        }.getOrThrow()
        seller.startFlow(RevertEncumberedTokensFlow(otherSignedTokensOfferTx.id)).apply {
            network.runNetwork()
        }.getOrThrow()

        val buyerBalanceAfterRevert = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val otherBuyerBalanceAfterRevert = otherBuyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        assertTrue(buyerBalanceAfterOffer < buyerInitialBalance)
        assertTrue(buyerBalanceAfterRevert == buyerInitialBalance)
        assertTrue(otherBuyerBalanceAfterOffer < otherBuyerInitialBalance)
        assertTrue(otherBuyerBalanceAfterRevert == otherBuyerInitialBalance)
    }

    @Test
    fun `revert steps between four parties by encumbered tx receiver with multiple bids and a winner`() {
        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()
        val otherBuyerParty = otherBuyer.info.chooseIdentity()

        val artworkState = issueArtwork(gallery)
        buyer.startFlow(IssueTokensFlow(20.GBP, buyerParty)).apply { network.runNetwork() }
        otherBuyer.startFlow(IssueTokensFlow(20.GBP, otherBuyerParty)).apply { network.runNetwork() }

        // all balances before the auction starts
        val sellerInitialBalance = seller.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val buyerInitialBalance = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val otherBuyerInitialBalance = otherBuyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())

        val verifiedDraftTx =
            bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                network.runNetwork()
            }.getOrThrow().second
        val otherVerifiedDraftTx =
            otherBidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                network.runNetwork()
            }.getOrThrow().second

        val signedTokensOfferTx =
            buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).apply {
                network.runNetwork()
            }.getOrThrow()
        val otherSignedTokensOfferTx =
            otherBuyer.startFlow(OfferEncumberedTokensFlow(sellerParty, otherVerifiedDraftTx, 10.GBP)).apply {
                network.runNetwork()
            }.getOrThrow()

        // all balances after initial art offer
        val buyerBalanceAfterOffer = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val otherBuyerBalanceAfterOffer = otherBuyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val sellerBalanceAfterOffers = seller.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()

        // Accept one of the bids (bidder win, otherBidder lose)
        val signedArtTransferTx = gallery.startFlow(SignAndFinalizeTransferOfOwnership(verifiedDraftTx.tx)).apply {
            network.runNetwork()
        }.getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        // claim winning bidder's tokens
        seller.startFlow(UnlockEncumberedTokensFlow(signedTokensOfferTx.id, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        // early-revert token to losing bidder
        seller.startFlow(RevertEncumberedTokensFlow(otherSignedTokensOfferTx.id)).apply {
            network.runNetwork()
        }.getOrThrow()


        // get new balances and ownerships
        val sellerBalanceAfterUnlock = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val buyerBalanceAfterUnlock = buyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val otherBuyerBalanceAfterRevert = otherBuyer.startFlow(GetBalanceFlow(GBP)).also { network.runNetwork() }.getOrThrow()
        val artworkItemGallery = queryArtworkState(gallery, false)
        val artworkItemBidder = queryArtworkState(bidder, false)
        val artworkItemOtherBidder = queryArtworkState(otherBidder, false)

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

    @Test
    fun `test auction currencies`() {
        AuctionCurrency.getInstance("GBP")
        AuctionCurrency.getInstance("CBDC")
    }

    private fun issueArtwork(node: StartedMockNode): ArtworkState {
        val flow = IssueArtworkFlow(ArtworkId.randomUUID(), Instant.now().plusSeconds(5000L))
        val future: Future<ArtworkState> = node.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }

    private fun queryArtworkState(party: StartedMockNode, all: Boolean): ArtworkState? {
        val stateStatus = if (all) Vault.StateStatus.ALL else Vault.StateStatus.UNCONSUMED
        val queryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(stateStatus)
        return party.services.vaultService.queryBy(
            ArtworkState::class.java,
            queryCriteria
        ).states.singleOrNull()?.state?.data
    }
}

