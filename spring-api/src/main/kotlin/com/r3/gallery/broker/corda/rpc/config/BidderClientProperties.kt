package com.r3.gallery.broker.corda.rpc.config

import com.r3.gallery.api.CordaRPCNetwork
import org.springframework.stereotype.Component

/**
 * Connection and credential properties for Bidders
 */

@Component("ArtNetworkBidderProperties")
class ArtNetworkBidderProperties(properties: RpcProperties) : ClientProperties {
    override var clients: List<NetworkClientConfig> =
        listOf(
            NetworkClientConfig(
                properties.bobX500,
                CordaRPCNetwork.AUCTION,
                properties.auction.url.bob,
                properties.username,
                properties.password
            ),
            NetworkClientConfig(
                properties.charlieX500,
                CordaRPCNetwork.AUCTION,
                properties.auction.url.charlie,
                properties.username,
                properties.password
            )
        )
}

@Component("TokenNetworkBidderProperties")
class TokenNetworkBidderProperties(properties: RpcProperties) : ClientProperties {
    override var clients: List<NetworkClientConfig> =
        listOf(
            NetworkClientConfig(
                properties.bobX500,
                CordaRPCNetwork.GBP,
                properties.gbp.url.bob,
                properties.username,
                properties.password
            ),
            NetworkClientConfig(
                properties.charlieX500,
                CordaRPCNetwork.CBDC,
                properties.cbdc.url.charlie,
                properties.username,
                properties.password
            )
        )
}