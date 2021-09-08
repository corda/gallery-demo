package com.r3.gallery.integrationTest

import net.corda.core.identity.Party
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import java.util.*

abstract class BasicAbstractFlowTest {
    lateinit var network: MockNetwork

    lateinit var seller: StartedMockNode
    lateinit var buyer: StartedMockNode

    lateinit var sellingParty: Party
    lateinit var receivingParty: Party

    val tokenIdentifier = UUID.randomUUID()

    val mockNetworkParameters = MockNetworkParameters(
        networkParameters = testNetworkParameters(minimumPlatformVersion = 5),
        cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.r3.gallery.contracts"),
            TestCordapp.findCordapp("com.r3.gallery.workflows")
        ),
        notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME))
    )

    @Before
    open fun setup() {
        network = MockNetwork(mockNetworkParameters)

        seller = network.createPartyNode(ALICE_NAME)
        buyer = network.createPartyNode(BOB_NAME)

        sellingParty = seller.info.legalIdentities.single()
        receivingParty = seller.info.legalIdentities.single()
    }

    @After
    open fun teardown() {
        network.stopNodes()
    }
}