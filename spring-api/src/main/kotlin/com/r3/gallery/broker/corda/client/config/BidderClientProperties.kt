package com.r3.gallery.broker.corda.client.config

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import org.springframework.stereotype.Component

/**
 * Connection and credential properties for Bidders
 */

@Component("ArtNetworkBidderProperties")
class ArtNetworkBidderProperties(properties: RpcProperties) : ClientProperties {
    override var clients: List<NetworkClientConfig> =
        listOf(
            NetworkClientConfig(
                "bob",
                CordaRPCNetwork.AUCTION,
                properties.auction.url.bob,
                properties.username,
                properties.password
            ),
            NetworkClientConfig(
                "charlie",
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
                "bob",
                CordaRPCNetwork.GBP,
                properties.gbp.url.bob,
                properties.username,
                properties.password
            ),
            NetworkClientConfig(
                "charlie",
                CordaRPCNetwork.CBDC,
                properties.cbdc.url.charlie,
                properties.username,
                properties.password
            )
        )
}