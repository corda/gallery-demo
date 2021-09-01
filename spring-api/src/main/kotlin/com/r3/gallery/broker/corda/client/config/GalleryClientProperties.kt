package com.r3.gallery.broker.corda.client.config

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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

@Component("TokenNetworkGalleryProperties")
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