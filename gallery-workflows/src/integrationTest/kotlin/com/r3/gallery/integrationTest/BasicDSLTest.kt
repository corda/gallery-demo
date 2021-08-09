package com.r3.gallery.integrationTest

import com.r3.gallery.workflows.BasicFlowInitiator
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.driver.internal.incrementalPortAllocation
import net.corda.testing.node.TestCordapp
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BasicDSLTest {

    lateinit var seller: NodeHandle
    lateinit var buyer: NodeHandle

    lateinit var sellingParty: Party
    lateinit var buyingParty: Party

    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(
            portAllocation = incrementalPortAllocation(),
            startNodesInProcess = true,
            cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.r3.gallery.contracts"),
                TestCordapp.findCordapp("com.r3.gallery.workflows")
            ),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 5, notaries = emptyList())
        )
    ) {
        seller = startNode(providedName = ALICE_NAME).getOrThrow()
        buyer = startNode(providedName = BOB_NAME).getOrThrow()

        sellingParty = seller.nodeInfo.legalIdentities.single()
        buyingParty = buyer.nodeInfo.legalIdentities.single()

        val start = Instant.now()
        test()
        val end = Instant.now()
        val duration = Duration.between(start, end)

        println("Test took ${duration.toString()}")
    }

    @Test
    fun `basic transaction`() = withDriver {
        println("creating transaction")
        val result = seller.rpc.startFlowDynamic(
            BasicFlowInitiator::class.java,
            buyingParty
        ).returnValue.getOrThrow()

        assert(result.tx.outputStates.size == 1)
    }

}
