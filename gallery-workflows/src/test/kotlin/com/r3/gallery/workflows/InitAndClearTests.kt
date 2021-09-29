package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.api.ArtworkId
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import com.r3.gallery.workflows.internal.issueArtwork
import com.r3.gallery.workflows.internal.mockNetwork
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
        network = mockNetwork()
        seller = network.createPartyNode()
        gallery = network.createPartyNode()
        bidder = network.createPartyNode()
        buyer = network.createPartyNode()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `burn tokens clears issued`() {
        val buyerParty = buyer.info.chooseIdentity()

        buyer.startFlow(IssueTokensFlow(1000.GBP, buyerParty)).getOrThrow()

        var balance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assert(balance == 1000.GBP)

        // burn tokens
        buyer.startFlow(BurnTokens("GBP")).getOrThrow()

        balance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assert(balance == 0.GBP)
    }

    @Test
    fun `burn tokens clears encumbered`() {

        val galleryParty = gallery.info.chooseIdentity()
        val sellerParty = seller.info.chooseIdentity()
        val buyerParty = buyer.info.chooseIdentity()

        // issue to buyer and seller tokens
        buyer.startFlow(IssueTokensFlow(1000.GBP, buyerParty))
        seller.startFlow(IssueTokensFlow(1000.GBP, sellerParty))

        var balance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        var sellerBalance = seller.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assert(balance == 1000.GBP)
        assert(sellerBalance == 1000.GBP)

        // issue an artwork state to encumber tokens on
        val artworkState = gallery.issueArtwork()
        val artworkLinearId = UniqueIdentifier.fromString(artworkState.linearId.toString())
        val verifiedDraftTx =
                bidder.startFlow(RequestDraftTransferOfOwnershipFlow(galleryParty, artworkLinearId)).getOrThrow()

        // encumber some tokens
        buyer.startFlow(OfferEncumberedTokensFlow(sellerParty, verifiedDraftTx, 10.GBP)).getOrThrow()

        balance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        sellerBalance = seller.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assert(balance == 990.GBP)
        assert(sellerBalance == 1000.GBP)

        // BURN tokens from both perspectives
        buyer.startFlow(BurnTokens("GBP")).getOrThrow()
        seller.startFlow(BurnTokens("GBP")).getOrThrow()

        balance = buyer.startFlow(GetBalanceFlow(GBP)).getOrThrow()
        sellerBalance = seller.startFlow(GetBalanceFlow(GBP)).getOrThrow()

        assert(balance == 0.GBP)
        assert(sellerBalance == 0.GBP)
    }
}