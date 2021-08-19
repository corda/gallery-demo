package com.r3.gallery.workflows

import com.r3.gallery.states.ArtworkState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.workflows.getCashBalance
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
//import kotlin.test.assertEquals
//import kotlin.test.assertNotNull

class SwapTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

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
        val artworkId = issueArtwork()
        val buyer = b.services.myInfo.legalIdentities.first()
        val flow = GetDraftTransferOfOwnership(artworkId, buyer)
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

        val artworkId = issueArtwork()
        val buyer = b.services.myInfo.legalIdentities.first()
        val flow = ShareDraftTransferOfOwnershipFlow(artworkId, buyer)
        val future: Future<Boolean> = a.startFlow(flow)
        network.runNetwork()
        val otherPartyAccepted = future.getOrThrow()
        assertTrue(otherPartyAccepted)
    }


    private fun issueArtwork(): UniqueIdentifier {
        val epoch = Instant.now().epochSecond
        val flow = IssueArtworkFlow(description = "test artwork $epoch", url = "http://www.google.com/search?q=$epoch")
        val future: Future<UniqueIdentifier> = a.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }

    private fun verifyDraftTransferOfOwnership(wtx: WireTransaction, artworkId: UniqueIdentifier, buyer: Party) {
        val artworkState = wtx.outputStates
                .filter { it::class.java == ArtworkState::class.java }
                .map { it as ArtworkState }
                .singleOrNull { it.linearId == artworkId }
                ?: throw FlowException("Shared tx contains no artwork matching the artwork identifier $artworkId")

        if(artworkState.owner != buyer) {
            throw FlowException("Shared tx owner ${artworkState.owner} does not match the expected owner $buyer")
        }
    }
}
