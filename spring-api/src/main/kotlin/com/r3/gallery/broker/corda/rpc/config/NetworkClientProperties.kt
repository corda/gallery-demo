package com.r3.gallery.broker.corda.rpc.config

import com.r3.gallery.api.CordaRPCNetwork
import org.springframework.stereotype.Component

/**
 * Stores connection configurations parsed from application.properties to represent AuctionNetwork
 */
@Component("AuctionNetworkProperties")
class AuctionNetworkProperties(properties: RpcProperties) : ClientProperties {

    override var clients: List<NetworkClientConfig> =
        listOf(
            NetworkClientConfig(
                properties.aliceX500,
                CordaRPCNetwork.AUCTION,
                properties.auction.url.alice,
                properties.username,
                properties.password
            ),
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

/**
 * Stores connection configurations parsed from application.properties to represent GbpNetwork
 */
@Component("GbpNetworkProperties")
class GbpNetworkProperties(properties: RpcProperties) : ClientProperties {

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
                properties.aliceX500,
                CordaRPCNetwork.GBP,
                properties.gbp.url.alice,
                properties.username,
                properties.password
            )
        )
}

/**
 * Stores connection configurations parsed from application.properties to represent CbdcNetwork
 */
@Component("CbdcNetworkProperties")
class CbdcNetworkProperties(properties: RpcProperties) : ClientProperties {

    override var clients: List<NetworkClientConfig> =
        listOf(
            NetworkClientConfig(
                properties.aliceX500,
                CordaRPCNetwork.CBDC,
                properties.cbdc.url.alice,
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