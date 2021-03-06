package com.r3.gallery.workflows

import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import com.r3.gallery.workflows.internal.mockNetwork
import com.r3.gallery.workflows.token.GetBalanceFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IssueTokenFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = mockNetwork()
        a = network.createPartyNode()
        b = network.createPartyNode()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `can issue tokens to a party`() {
        val aParty = a.info.chooseIdentity()

        val issueFlow = IssueTokensFlow(1000, GBP.tokenIdentifier, aParty)
        val stx = a.startFlow(issueFlow).getOrThrow()

        val balance = a.services.vaultService.tokenBalance(GBP)
        assertNotNull(stx)
        assertEquals(10.GBP, balance)
    }

    @Test
    fun `can get balance`() {
        val aParty = a.info.chooseIdentity()
        val issueFlow = IssueTokensFlow(1000, GBP.tokenIdentifier, aParty)
        a.startFlow(issueFlow).getOrThrow()

        val balanceFlow = GetBalanceFlow(GBP)
        val balance = a.startFlow(balanceFlow).getOrThrow()

        assertEquals(10.GBP, balance)
    }
}