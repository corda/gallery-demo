package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.USD
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalanceForIssuer
import com.r3.gallery.states.EmptyState
import com.r3.gallery.worskflows.IssueTokensFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Future

class IssueTokenFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.r3.gallery.contracts"),
            TestCordapp.findCordapp("com.r3.gallery.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
        )))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `DummyTest`() {
        val aParty = a.info.chooseIdentity()

        val flow = IssueTokensFlow(10, USD.tokenIdentifier, aParty)
        val future: Future<SignedTransaction> = a.startFlow(flow)
        network.runNetwork()

        val state1 = a.services.vaultService.tokenBalance(USD)
        val state2 = a.services.vaultService.tokenBalanceForIssuer(USD, aParty)
    }
}