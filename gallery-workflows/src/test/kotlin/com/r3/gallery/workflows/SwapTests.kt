package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.USD
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkOwnership
import com.r3.gallery.states.ArtworkState
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
import java.util.concurrent.Future


class SwapTests {
    private lateinit var network: MockNetwork
    private lateinit var seller: StartedMockNode
    private lateinit var buyer1: StartedMockNode
    private lateinit var buyer2: StartedMockNode

    private lateinit var gallery: StartedMockNode
    private lateinit var bidder: StartedMockNode
    private lateinit var buyer: StartedMockNode

    @Before
    fun setup() {
        val notaries = listOf(
            MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB")),
            MockNetworkNotarySpec(CordaX500Name("Notary", "Zurich", "CH"))
        )

//        val (networkSendManuallyPumped, threadPerNode, servicePeerAllocationStrategy, notarySpecs, networkParameters, cordappsForAllNodes) = MockNetworkParameters().withNotarySpecs(
//            Arrays.asList()
//        )
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
        buyer1 = network.createPartyNode()
        buyer2 = network.createPartyNode()
        gallery = network.createPartyNode()
        bidder = network.createPartyNode()
        buyer = network.createPartyNode()

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `can issue artwork`() {
        //val flow = IssueArtworkFlow(description = "test artwork $epoch", url = "http://www.google.com/search?q=$epoch")
        val flow = IssueArtworkFlow(ArtworkId.randomUUID())
        val artworkOwnership = seller.startFlow(flow).also { network.runNetwork() }.getOrThrow()
        // TODO
    }

    @Test
    fun `can draft transfer tx`() {
        val artworkOwnership: ArtworkOwnership = issueArtwork(seller)
        val sellerParty = seller.info.chooseIdentity()
        val buyer1Party = buyer1.info.chooseIdentity()

//        val artTransferTx = seller.startFlow(BuildDraftTransferOfOwnership(artworkOwnership, buyer1Party)).apply {
//            network.runNetwork()
//        }.getOrThrow()
//
//        val signedArtTransferTx = seller.startFlow(SignAndFinalizeTransferOfOwnership(artTransferTx)).apply {
//            network.runNetwork()
//        }.getOrThrow()

        val artworkLinearId = UniqueIdentifier.fromString(artworkOwnership.cordaReference.toString())
        val artTransferTx2 = seller.startFlow(CreateDraftTransferOfOwnershipFlow(artworkLinearId, buyer1Party)).apply {
            network.runNetwork()
        }.getOrThrow()

        val signedArtTransferTx2 = seller.startFlow(SignAndFinalizeTransferOfOwnership(artTransferTx2)).apply {
            network.runNetwork()
        }.getOrThrow()

    }

    @Test
    fun `swap steps`() {
        val artworkId = issueArtwork(seller)
        val sellerParty = seller.info.chooseIdentity()
        val buyer1Party = buyer1.info.chooseIdentity()

        buyer1.startFlow(IssueTokensFlow(20.USD, buyer1Party)).apply {
            network.runNetwork()
        }

        val artTransferTx = seller.startFlow(BuildDraftTransferOfOwnership(artworkId, buyer1Party)).apply {
            network.runNetwork()
        }.getOrThrow()

        val lockStateRef =
            buyer1.startFlow(OfferEncumberedTokensFlow(artTransferTx, sellerParty, 10.USD)).apply {
                network.runNetwork()
            }.getOrThrow()

        val signedArtTransferTx = seller.startFlow(SignAndFinalizeTransferOfOwnership(artTransferTx)).apply {
            network.runNetwork()
        }.getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        seller.startFlow(UnlockEncumberedTokensFlow(lockStateRef, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        val artworkItemA = queryArtworkState(seller, false)
        assertNull(artworkItemA)

        val artworkItemB = queryArtworkState(buyer1, false)
        assertNotNull(artworkItemB)
        assertEquals(buyer1Party, buyer1.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

        val aBalance = seller.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
        val bBalance = buyer1.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
    }

    @Test
    fun `swap steps with four parties`() {

        val galleryParty = gallery.info.chooseIdentity()
        val bidderParty = bidder.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkId = issueArtwork(gallery)
        buyer.startFlow(IssueTokensFlow(20.USD, buyerParty)).apply {
            network.runNetwork()
        }

        val artTransferTx = gallery.startFlow(BuildDraftTransferOfOwnership(artworkId, bidderParty)).apply {
            network.runNetwork()
        }.getOrThrow()

        val lockStateRef =
            buyer.startFlow(OfferEncumberedTokensFlow(artTransferTx, sellerParty, 10.USD)).apply {
                network.runNetwork()
            }.getOrThrow()

        val signedArtTransferTx = gallery.startFlow(SignAndFinalizeTransferOfOwnership(artTransferTx)).apply {
            network.runNetwork()
        }.getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        seller.startFlow(UnlockEncumberedTokensFlow(lockStateRef, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        val artworkItemGallery = queryArtworkState(gallery, false)
        assertNull(artworkItemGallery)

        val artworkItemBidder = queryArtworkState(bidder, false)
        assertNotNull(artworkItemBidder)
//        assertEquals(buyer1Party, buyer1.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))
//
//        val aBalance = seller.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
//        val bBalance = buyer1.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
    }

    @Test
    fun `swap steps with four parties 3`() {

        val galleryParty = gallery.info.chooseIdentity()
        val bidderParty = bidder.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        val artworkId = issueArtwork(gallery)
        buyer.startFlow(IssueTokensFlow(20.USD, buyerParty)).apply {
            network.runNetwork()
        }

        val artTransferTx = gallery.startFlow(BuildDraftTransferOfOwnership(artworkId, bidderParty)).apply {
            network.runNetwork()
        }.getOrThrow()

        val lockStateRef =
            buyer.startFlow(OfferEncumberedTokensFlow(artTransferTx, sellerParty, 10.USD)).apply {
                network.runNetwork()
            }.getOrThrow()

        val signedArtTransferTx = gallery.startFlow(SignAndFinalizeTransferOfOwnership(artTransferTx)).apply {
            network.runNetwork()
        }.getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        seller.startFlow(UnlockEncumberedTokensFlow(lockStateRef, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        val artworkItemGallery = queryArtworkState(gallery, false)
        assertNull(artworkItemGallery)

        val artworkItemBidder = queryArtworkState(bidder, false)
        assertNotNull(artworkItemBidder)
//        assertEquals(buyer1Party, buyer1.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))
//
//        val aBalance = seller.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
//        val bBalance = buyer1.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
    }

    @Test
    fun `swap steps x-net`() {
        val artworkId = issueArtwork(seller)
        val sellerParty = seller.info.chooseIdentity()
        val buyer1Party = buyer1.info.chooseIdentity()

        buyer1.startFlow(IssueTokensFlow(20.USD, buyer1Party)).apply {
            network.runNetwork()
        }

        val artworkLinearId = UniqueIdentifier.fromString(artworkId.cordaReference.toString())
        val draft = seller.startFlow(CreateDraftTransferOfOwnershipFlow2(artworkLinearId, buyer1Party)).apply {
            network.runNetwork()
        }.getOrThrow()

        val atrTransferTx = draft.first
        val lockState = draft.second

        val lockStateRef =
            buyer1.startFlow(OfferEncumberedTokensFlow2(lockState, sellerParty, 10.USD)).apply {
                network.runNetwork()
            }.getOrThrow()

        val signedArtTransferTx = seller.startFlow(SignAndFinalizeTransferOfOwnership(atrTransferTx)).apply {
            network.runNetwork()
        }.getOrThrow()

        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        seller.startFlow(UnlockEncumberedTokensFlow(lockStateRef, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        val artworkItemA = queryArtworkState(seller, false)
        assertNull(artworkItemA)

        val artworkItemB = queryArtworkState(buyer1, false)
        assertNotNull(artworkItemB)
        assertEquals(buyer1Party, buyer1.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

        val aBalance = seller.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
        val bBalance = buyer1.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
    }

    @Test
    fun `can perform swap over x-network`() {
        val artworkId = issueArtwork(seller)
        val sellerParty = seller.info.chooseIdentity()
        val buyer1Party = buyer1.info.chooseIdentity()

        buyer1.startFlow(IssueTokensFlow(20.USD, buyer1Party)).apply {
            network.runNetwork()
        }

        // ControllerService::PlaceBid [

        // bidder/art-net <-> gallery/art-net (*)
        val artTransferTx = buyer1.startFlow(PlaceBidFlow(sellerParty, artworkId, 10, USD.tokenIdentifier)).apply {
            network.runNetwork()
        }.getOrThrow()

        // bidder/token-net <-> gallery/token-net
        val lockStateRef =
            buyer1.startFlow(OfferEncumberedTokensFlow(artTransferTx, sellerParty, 10.USD)).apply {
                network.runNetwork()
            }.getOrThrow()
// encumberedtokens
        // controllingNotary, txHash, lockstate output index

        // ] ControllerService::PlaceBid

        //

        // ControllerService::AcceptBid [

        // gallery/art-net <-> bidder/art-net  (*)
        val signedArtTransferTx = seller.startFlow(SignAndFinalizeTransferOfOwnership(artTransferTx)).apply {
            network.runNetwork()
        }.getOrThrow()


        val requiredSignature = signedArtTransferTx.getNotaryTransactionSignature()

        // gallery/token-net <-> bidder/token-net
        seller.startFlow(UnlockEncumberedTokensFlow(lockStateRef, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        // ] ControllerService::PlaceBid

        val artworkItemA = queryArtworkState(seller, false)
        val artworkItemB = queryArtworkState(buyer1, false)

        assertNull(artworkItemA)
        assertNotNull(artworkItemB)
        assertEquals(buyer1Party, buyer1.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

        val sellerBalance = seller.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
        val buery1Balance = buyer1.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
    }

    @Ignore
    @Test
    fun `share offer of encumbered tokens2`() {
        val artworkId = issueArtwork(seller)
        val sellerParty = seller.info.chooseIdentity()
        val buyer1Party = buyer1.info.chooseIdentity()
        val buyer2Party = buyer2.info.chooseIdentity()

        buyer1.startFlow(IssueTokensFlow(11.USD, buyer1Party))
        buyer2.startFlow(IssueTokensFlow(11.USD, buyer2Party))
        network.runNetwork()

        val artTransferTx1 = seller.startFlow(BuildDraftTransferOfOwnership(artworkId, buyer1Party)).also {
            network.runNetwork()
        }.getOrThrow()

        val artTransferTx2 = seller.startFlow(BuildDraftTransferOfOwnership(artworkId, buyer2Party)).also {
            network.runNetwork()
        }.getOrThrow()

        buyer1.startFlow(OfferEncumberedTokensFlow(artTransferTx1, sellerParty, 10.USD))
        network.runNetwork()

        // TODO: accept bid buyer 1

        buyer2.startFlow(OfferEncumberedTokensFlow(artTransferTx2, sellerParty, 10.USD))
        network.runNetwork()

        // TODO: accept bid buyer 2 ? redeem ?

        val artworkItemA = queryArtworkState(seller, false)
        val artworkItemB = queryArtworkState(buyer1, false)
        val artworkItemC = queryArtworkState(buyer2, false)

        val sellerBalance = seller.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
        val buyer1Balance = buyer1.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
        val buyer2Balance = buyer2.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()

        assertNull(artworkItemA)
        assertNull(artworkItemC)
        assertNotNull(artworkItemB)
        assertEquals(buyer1Party, buyer1.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

        assertEquals(1.USD, buyer1Balance)
        assertEquals(11.USD, buyer2Balance)
    }

    private fun issueArtwork(node: StartedMockNode): ArtworkOwnership {
        //val epoch = Instant.now().epochSecond
        //val flow = IssueArtworkFlow(description = "test artwork $epoch", url = "http://www.google.com/search?q=$epoch")
        val flow = IssueArtworkFlow(ArtworkId.randomUUID())
        val future: Future<ArtworkOwnership> = node.startFlow(flow)
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

