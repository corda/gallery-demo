package com.r3.gallery.broker.corda.rpc.config

import com.r3.gallery.api.CordaRPCNetwork
import org.springframework.stereotype.Component

/**
 * Connection and credential properties for Gallery
 */
@Component("ArtNetworkGalleryProperties")
class ArtNetworkGalleryProperties(properties: RpcProperties) : ClientProperties {

    override var clients: List<NetworkClientConfig> =
        listOf(
            NetworkClientConfig(
                properties.aliceX500,
                CordaRPCNetwork.AUCTION,
                properties.auction.url.alice,
                properties.username,
                properties.password
            )
        )
}

@Component("TokenNetworkSellerProperties")
class TokenNetworkGalleryProperties(properties: RpcProperties) : ClientProperties {
    override var clients: List<NetworkClientConfig> =
        listOf(
            NetworkClientConfig(
                properties.aliceX500,
                CordaRPCNetwork.GBP,
                properties.gbp.url.alice,
                properties.username,
                properties.password
            ),
            NetworkClientConfig(
                properties.aliceX500,
                CordaRPCNetwork.CBDC,
                properties.cbdc.url.alice,
                properties.username,
                properties.password
            )
        )
}