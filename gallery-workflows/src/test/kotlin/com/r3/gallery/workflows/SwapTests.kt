package com.r3.gallery.workflows

import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.workflows.getCashBalance
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.util.*
import java.util.concurrent.Future

class SwapTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    private lateinit var c: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.r3.gallery.contracts"),
                TestCordapp.findCordapp("com.r3.gallery.workflows"),
                TestCordapp.findCordapp("net.corda.finance.schemas"),
                TestCordapp.findCordapp("net.corda.finance.contracts.asset")
        )))
        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

//    @Test
//    fun `DummyTest`() {
//        val flow = BasicFlowInitiator(b.info.legalIdentities[0])
//        val future: Future<SignedTransaction> = a.startFlow(flow)
//        network.runNetwork()
//
//        //successful query means the state is stored at node b's vault. Flow went through.
//        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
//        val state = b.services.vaultService.queryBy(EmptyState::class.java, inputCriteria).states[0].state.data
//    }

    @Test
    fun `can issue cash to self`() {
        val amount = Amount(100, Currency.getInstance("USD"))
        val flow = SelfIssueCashFlow(amount)
        val future: Future<Cash.State> = a.startFlow(flow)
        network.runNetwork()
        val balance = a.services.getCashBalance(Currency.getInstance("USD"))
        assertEquals(amount, balance)
    }

    @Test
    fun `can issue artwork items`() {
        val flow = IssueArtworkFlow(description = "test artwork", url = "http://www.google.com")
        val future: Future<UniqueIdentifier> = a.startFlow(flow)
        network.runNetwork()

        val artworkId = future.getOrThrow()
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
        val state = a.services.vaultService.queryBy(ArtworkState::class.java, inputCriteria)
                .states.singleOrNull { x -> x.state.data.linearId == artworkId }.let { x -> x!!.state.data }

        assertNotNull(state)
        assertEquals("test artwork", state.description)
        assertEquals("http://www.google.com", state.url)
    }

    @Test
    fun `can draft transfer of ownership`() {
        val artworkId = issueArtwork(a)
        val buyer = b.services.myInfo.legalIdentities.first()
        val flow = BuildDraftTransferOfOwnership(artworkId, buyer)
        val future: Future<WireTransaction> = a.startFlow(flow)
        network.runNetwork()
        val wireTransaction = future.getOrThrow()
        assertDoesNotThrow {
            verifyDraftTransferOfOwnership(wireTransaction, artworkId, buyer)
        }
    }

    @Test
    fun `share unsigned transfer of ownership`() {
        // a wants to buy ART from b at cost $
        // b approves
        // a sends $ tx to b unsigned
        // b sends encumbered ART tx to a

        val artworkId = issueArtwork(a)
        val buyer = b.services.myInfo.legalIdentities.first()
        val flow = ShareDraftTransferOfOwnershipFlow(artworkId, buyer)
        val future: Future<Boolean> = a.startFlow(flow)
        network.runNetwork()
        val otherPartyAccepted = future.getOrThrow()
        assertTrue(otherPartyAccepted)
    }

    @Test
    fun `share offer of encumbered tokens`() {
        // a wants to buy ART from b at cost $
        // b approves
        // a sends ART tx to b unsigned
        // b sends encumbered $ tx to a

        val artworkId = issueArtwork(a)
        val buyer = b.info.chooseIdentity()
        val seller = a.info.chooseIdentity()
        val usdCurrency = Currency.getInstance("USD")

        val cashState = b.startFlow(SelfIssueCashFlow(Amount(11, usdCurrency))).apply {
            network.runNetwork()
        }.getOrThrow()

        val artTransferTx = a.startFlow(BuildDraftTransferOfOwnership(artworkId, buyer)).apply {
            network.runNetwork()
        }.getOrThrow()


        val future: Future<SignedTransaction> = b.startFlow(OfferEncumberedTokensFlow(artTransferTx, seller, Amount(10, Currency.getInstance("USD"))))
        network.runNetwork()

        val queryArtworkState = { party: StartedMockNode, all: Boolean ->
            val qc = QueryCriteria.VaultQueryCriteria().withStatus(if (all) Vault.StateStatus.ALL else Vault.StateStatus.UNCONSUMED)
            party.services.vaultService.queryBy(ArtworkState::class.java, qc).states.singleOrNull()?.state?.data
        }

        val artworkItemA = queryArtworkState(a, false)
        assertNull(artworkItemA)

        val artworkItemB = queryArtworkState(b, false)
        assertNotNull(artworkItemB)
        assertEquals(buyer, b.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

//        val notarisedArtworkItem = queryArtworkState(network.defaultNotaryNode, true)
//        assertNotNull(notarisedArtworkItem)
//        assertEquals(network.defaultNotaryNode.info.legalIdentities.first(),
//            network.defaultNotaryNode.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

        val aBalance = a.services.getCashBalance(usdCurrency)

        val bBalance = b.services.getCashBalance(usdCurrency)
        assert(bBalance.quantity > 0)
    }

    @Test
    fun `share offer of encumbered tokens2`() {
        // a wants to buy ART from b at cost $
        // b approves
        // a sends ART tx to b unsigned
        // b sends encumbered $ tx to a

        val artworkId = issueArtwork(a)
        val seller = a.info.chooseIdentity()
        val buyer = b.info.chooseIdentity()
        val buyer2 = b.info.chooseIdentity()

        val usdCurrency = Currency.getInstance("USD")

        val cashState = b.startFlow(SelfIssueCashFlow(Amount(11, usdCurrency))).apply {
            network.runNetwork()
        }.getOrThrow()

        val artTransferTx = a.startFlow(BuildDraftTransferOfOwnership(artworkId, buyer)).apply {
            network.runNetwork()
        }.getOrThrow()

        val artTransferTx2 = a.startFlow(BuildDraftTransferOfOwnership(artworkId, buyer2)).apply {
            network.runNetwork()
        }.getOrThrow()

        val future: Future<SignedTransaction> = b.startFlow(OfferEncumberedTokensFlow(artTransferTx, seller, Amount(10, Currency.getInstance("USD"))))
        network.runNetwork()

        val queryArtworkState = { party: StartedMockNode, all: Boolean ->
            val qc = QueryCriteria.VaultQueryCriteria().withStatus(if (all) Vault.StateStatus.ALL else Vault.StateStatus.UNCONSUMED)
            party.services.vaultService.queryBy(ArtworkState::class.java, qc).states.singleOrNull()?.state?.data
        }

        val artworkItemA = queryArtworkState(a, false)
        assertNull(artworkItemA)

        val artworkItemB = queryArtworkState(b, false)
        assertNotNull(artworkItemB)
        assertEquals(buyer, b.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

//        val notarisedArtworkItem = queryArtworkState(network.defaultNotaryNode, true)
//        assertNotNull(notarisedArtworkItem)
//        assertEquals(network.defaultNotaryNode.info.legalIdentities.first(),
//            network.defaultNotaryNode.services.identityService.wellKnownPartyFromAnonymous(artworkItemB!!.owner))

        val aBalance = a.services.getCashBalance(usdCurrency)

        val bBalance = b.services.getCashBalance(usdCurrency)
        assert(bBalance.quantity > 0)
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
}
