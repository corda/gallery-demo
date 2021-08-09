package com.r3.gallery.freighter

import com.r3.gallery.states.EmptyState
import com.r3.gallery.workflows.BasicFlowInitiator
import freighter.deployments.DeploymentContext
import freighter.deployments.NodeBuilder
import freighter.deployments.SingleNodeDeployment
import freighter.deployments.UnitOfDeployment
import freighter.machine.DeploymentMachineProvider
import freighter.machine.generateRandomString
import freighter.testing.DockerRemoteMachineBasedTest
import net.corda.core.utilities.getOrThrow
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.spi.ExtendedLogger
import org.junit.jupiter.api.Test
import utility.getOrThrow
import java.time.Duration

/**
 * Docker driven freighter tests for e2e transactions
 */
class GalleryFreighterTest : DockerRemoteMachineBasedTest() {
    companion object {
        val logger: ExtendedLogger = LogManager.getContext().getLogger(this::class.java.name)
    }

    private val galleryContractsCordapp = NodeBuilder.DeployedCordapp.fromClassPath("gallery-contracts", "test")
    private val galleryWorkflowsCordapp = NodeBuilder.DeployedCordapp.fromClassPath("gallery-workflows", "test")

    @Test
    fun `single tx from buyer to seller`() {
        start(DeploymentMachineProvider.DatabaseType.H2)
    }

    private fun start(db: DeploymentMachineProvider.DatabaseType) {
        val randomString = generateRandomString()
        val deploymentContext = DeploymentContext(machineProvider, nms, artifactoryUsername, artifactoryPassword)

        val deploymentResult = SingleNodeDeployment(
            NodeBuilder().withX500("O=PartyA, C=GB, L=LONDON, CN=$randomString")
                .withCordapp(galleryContractsCordapp)
                .withCordapp(galleryWorkflowsCordapp)
                .withDatabase(machineProvider.requestDatabase(db))
        ).withVersion(UnitOfDeployment.CORDA_4_7)
            .deploy(deploymentContext)

        val deploymentResult2 = SingleNodeDeployment(
            NodeBuilder().withX500("O=PartyB, C=GB, L=LONDON, CN=$randomString")
                .withCordapp(galleryContractsCordapp)
                .withCordapp(galleryWorkflowsCordapp)
                .withDatabase(machineProvider.requestDatabase(db))
        ).withVersion(UnitOfDeployment.CORDA_4_7)
            .deploy(deploymentContext)

        val sellerMachine = deploymentResult.getOrThrow()
        val buyerMachine = deploymentResult2.getOrThrow()

        val statesBefore = pollVault<EmptyState>(buyerMachine)

        sellerMachine.rpc {
            startFlowDynamic(
                BasicFlowInitiator::class.java,
                buyerMachine.identity()
            ).returnValue.getOrThrow(Duration.ofMinutes(1))
        }

        val statesAfter = pollVault<EmptyState>(buyerMachine)
        assert(statesBefore.size != statesAfter.size)
    }
}