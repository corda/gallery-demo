package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.USD
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.states.LockState
import com.r3.gallery.utils.getTransactionSignatureForParty
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import com.r3.gallery.workflows.token.GetBalanceFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.util.concurrent.Future

class SwapTests {
    private lateinit var network: MockNetwork
    private lateinit var seller: StartedMockNode
    private lateinit var buyer1: StartedMockNode
    private lateinit var buyer2: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.r3.gallery.contracts"),
                    TestCordapp.findCordapp("com.r3.gallery.workflows"),
                    TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
                )
            )
        )
        seller = network.createPartyNode()
        buyer1 = network.createPartyNode()
        buyer2 = network.createPartyNode()

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `can issue artwork items`() {
        val flow = IssueArtworkFlow(description = "test artwork", url = "http://www.google.com")
        val future: Future<UniqueIdentifier> = seller.startFlow(flow)
        network.runNetwork()

        val artworkId = future.getOrThrow()
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
        val state = seller.services.vaultService.queryBy(ArtworkState::class.java, inputCriteria)
            .states.singleOrNull { x -> x.state.data.linearId == artworkId }.let { x -> x!!.state.data }

        assertNotNull(state)
        assertEquals("test artwork", state.description)
        assertEquals("http://www.google.com", state.url)
    }

    @Test
    fun `can draft transfer of ownership`() {
        val artworkId = issueArtwork(seller)
        val buyer = buyer1.services.myInfo.legalIdentities.first()
        val flow = BuildDraftTransferOfOwnership(artworkId, buyer)
        val future: Future<WireTransaction> = seller.startFlow(flow)
        network.runNetwork()
        val wireTransaction = future.getOrThrow()
        assertDoesNotThrow {
            verifyDraftTransferOfOwnership(wireTransaction, artworkId, buyer)
        }
    }

    @Test
    fun `share offer of encumbered tokens fungible tokens`() {
        val artworkId = issueArtwork(seller)
        val sellerParty = seller.info.chooseIdentity()
        val buyer1Party = buyer1.info.chooseIdentity()

        buyer1.startFlow(IssueTokensFlow(20.USD, buyer1Party)).apply {
            network.runNetwork()
        }

        // gallery (seller) drafts a transfer of ownership for bidder party (buyer1Party)
        val artTransferTx = seller.startFlow(BuildDraftTransferOfOwnership(artworkId, buyer1Party)).apply {
            network.runNetwork()
        }.getOrThrow()

        val encumberedTx =
            buyer1.startFlow(OfferEncumberedTokensFlow(artTransferTx, sellerParty, 10.USD)).apply {
                network.runNetwork()
            }.getOrThrow()

        val lockStateAndRef = with(encumberedTx.tx.outRefsOfType(LockState::class.java).single()) {
            StateAndRef(TransactionState(state.data, state.contract, state.notary), ref)
        }

        val signedArtTransferTx = seller.startFlow(SignAndFinalizeTransferOfOwnership(lockStateAndRef, artTransferTx)).apply {
            network.runNetwork()
        }.getOrThrow()

        val controllingNotary = lockStateAndRef.state.data.controllingNotary
        val requiredSignature = signedArtTransferTx.getTransactionSignatureForParty(controllingNotary)

        seller.startFlow(UnlockPushedEncumberedDefinedTokenFlow(lockStateAndRef, requiredSignature)).apply {
            network.runNetwork()
        }.getOrThrow()

        val artworkItemA = queryArtworkState(seller, false)
        assertNull(artworkItemA)

        val artworkItemB = queryArtworkState(buyer1, false)
        assertNotNull(artworkItemB)
        assertEquals(buyer1Party, buyer1.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))


//        val notarisedArtworkItem = queryArtworkState(network.defaultNotaryNode, true)
//        assertNotNull(notarisedArtworkItem)
//        assertEquals(network.defaultNotaryNode.info.legalIdentities.first(),
//            network.defaultNotaryNode.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

        val aBalance = seller.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
        val bBalance = buyer1.startFlow(GetBalanceFlow(USD)).also { network.runNetwork() }.getOrThrow()
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

    // flow 1 (first half)
    @Ignore
    @Test
    fun `can place a bid`() {
        val artworkId = issueArtwork(seller)
        val sellerParty = seller.info.chooseIdentity()
        val buyer1Party = buyer1.info.chooseIdentity()

        buyer1.startFlow(IssueTokensFlow(11.USD, buyer1Party))
        buyer1.startFlow(IssueTokensFlow(11.USD, buyer1Party))

        val artTransferTx = buyer1.startFlow(PlaceBidFlow(sellerParty, artworkId, Amount(10, USD))).apply {
            network.runNetwork()
        }.getOrThrow()

        seller.startFlow(AcceptBidFlow(artTransferTx)).apply {
            network.runNetwork()
        }
    }

    // flow 1 (second half)
    @Ignore
    @Test
    fun `can offer encumbered tokens`() {
        val artworkId = issueArtwork(seller)
        val sellerParty = seller.info.chooseIdentity()
        val buyer1Party = buyer1.info.chooseIdentity()

        buyer1.startFlow(IssueTokensFlow(20.USD, buyer1Party)).also {
            network.runNetwork()
        }.getOrThrow()

        val artTransferTx = buyer1.startFlow(PlaceBidFlow(sellerParty, artworkId, Amount(10, USD))).also {
            network.runNetwork()
        }.getOrThrow()

        seller.startFlow(AcceptBidFlow(artTransferTx))
        network.runNetwork()
    }

    private fun issueArtwork(node: StartedMockNode): UniqueIdentifier {
        val epoch = Instant.now().epochSecond
        val flow = IssueArtworkFlow(description = "test artwork $epoch", url = "http://www.google.com/search?q=$epoch")
        val future: Future<UniqueIdentifier> = node.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }

    private fun verifyDraftTransferOfOwnership(wtx: WireTransaction, artworkId: UniqueIdentifier, buyer: Party) {
        val artworkState = wtx.outputStates
            .filter { it::class.java == ArtworkState::class.java }
            .map { it as ArtworkState }
            .singleOrNull { it.linearId == artworkId }
            ?: throw FlowException("Shared tx contains no artwork matching the artwork identifier $artworkId")

        if (artworkState.owner != buyer) {
            throw FlowException("Shared tx owner ${artworkState.owner} does not match the expected owner $buyer")
        }
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

