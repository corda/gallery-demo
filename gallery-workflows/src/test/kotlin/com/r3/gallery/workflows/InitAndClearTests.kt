package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import com.r3.gallery.workflows.token.BurnTokens
import com.r3.gallery.workflows.token.GetBalanceFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.concurrent.Future

class InitAndClearTests {

    private lateinit var network: MockNetwork
    private lateinit var seller: StartedMockNode
    private lateinit var gallery: StartedMockNode
    private lateinit var bidder: StartedMockNode
    private lateinit var buyer: StartedMockNode

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

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `burn tokens clears issued`() {

        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        buyer.startFlow(IssueTokensFlow(1000.GBP, buyerParty)).apply {
            network.runNetwork()
        }

        var balance = buyer.startFlow(GetBalanceFlow(GBP)).apply {
            network.runNetwork()
        }.toCompletableFuture().get()

        assert(balance == 1000.GBP)

        // burn tokens
        buyer.startFlow(BurnTokens("GBP")).apply {
            network.runNetwork()
        }

        balance = buyer.startFlow(GetBalanceFlow(GBP)).apply {
            network.runNetwork()
        }.toCompletableFuture().get()

        assert(balance == 0.GBP)
    }

    @Test
    fun `burn tokens clears encumbered`() {

        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        // issue to buyer and seller tokens
        buyer.startFlow(IssueTokensFlow(1000.GBP, buyerParty)).apply {
            network.runNetwork()
        }
        seller.startFlow(IssueTokensFlow(1000.GBP, sellerParty)).apply {
            network.runNetwork()
        }

        var balance = buyer.startFlow(GetBalanceFlow(GBP)).apply {
            network.runNetwork()
        }.getOrThrow()
        var sellerBalance = seller.startFlow(GetBalanceFlow(GBP)).apply {
            network.runNetwork()
        }.getOrThrow()

        assert(balance == 1000.GBP)
        assert(sellerBalance == 1000.GBP)

        // issue an artwork state to encumber tokens on
        val artworkState = issueArtwork(gallery)
        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
                bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).apply {
                    network.runNetwork()
                }.getOrThrow().second

        // encumber some tokens
        val signedTokensOfferTx =
                buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).apply {
                    network.runNetwork()
                }.getOrThrow()

        balance = buyer.startFlow(GetBalanceFlow(GBP)).apply {
            network.runNetwork()
        }.toCompletableFuture().get()
        sellerBalance = seller.startFlow(GetBalanceFlow(GBP)).apply {
            network.runNetwork()
        }.getOrThrow()

        assert(balance == 990.GBP)
        assert(sellerBalance == 1000.GBP)

        // BURN tokens from both perspectives
        buyer.startFlow(BurnTokens("GBP")).apply {
            network.runNetwork()
        }
        seller.startFlow(BurnTokens("GBP")).apply {
            network.runNetwork()
        }

        balance = buyer.startFlow(GetBalanceFlow(GBP)).apply {
            network.runNetwork()
        }.getOrThrow()
        sellerBalance = seller.startFlow(GetBalanceFlow(GBP)).apply {
            network.runNetwork()
        }.getOrThrow()

        assert(balance == 0.GBP)
        assert(sellerBalance == 0.GBP)
    }

    private fun issueArtwork(node: StartedMockNode): ArtworkState {
        val flow = IssueArtworkFlow(ArtworkId.randomUUID(), Instant.now().plusSeconds(5000L))
        val future: Future<ArtworkState> = node.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }
}