package com.r3.gallery.workflows.internal

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.*
import java.sql.Time
import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalUnit

internal fun StartedMockNode.issueArtwork(): ArtworkState {
    val flow = IssueArtworkFlow(ArtworkId.randomUUID(), Instant.now().plusSeconds(5000L))
    return this.startFlow(flow).getOrThrow()
}

internal fun StartedMockNode.queryArtworkState(artworkId: ArtworkId, all: Boolean): ArtworkState? {
    val stateStatus = if (all) Vault.StateStatus.ALL else Vault.StateStatus.UNCONSUMED
    val queryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(stateStatus)
    return this.services.vaultService.queryBy(
        ArtworkState::class.java,
        queryCriteria
    ).states.singleOrNull { it.state.data.artworkId == artworkId }?.state?.data
}

internal fun mockNetwork() : MockNetwork {
    val network = MockNetwork(
        MockNetworkParameters(
            cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.r3.gallery.contracts"),
                TestCordapp.findCordapp("com.r3.gallery.workflows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
            ),
            threadPerNode = true,
            networkSendManuallyPumped = false
        )
    )

    return network
}

internal fun moveClock(startedMockNodes: Set<StartedMockNode>, shiftInSeconds: Long){
    val now = Instant.now()
    startedMockNodes.forEach {
        (it.services.clock as TestClock).setTo(now.plusSeconds(shiftInSeconds))
    }
}