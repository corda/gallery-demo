package com.r3.gallery.broker.corda.client.config

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Connection and credential properties for Gallery
 */
@Component("ArtNetworkGalleryProperties")
class ArtNetworkGalleryProperties : ClientProperties {
    override var clients: List<NetworkClientConfig> =
        listOf(
            object : NetworkClientConfig() {
                override val nodeName: String = "alice"
                override var network: CordaRPCNetwork = CordaRPCNetwork.AUCTION
                @Value("\${auction.alice.rpc.url}")
                override lateinit var nodeUrl: String
                @Value("\${auction.alice.rpc.username}")
                override lateinit var nodeUsername: String
                @Value("\${auction.alice.rpc.password}")
                override lateinit var nodePassword: String
            }
        )
}

@Component("TokenNetworkGalleryProperties")
class TokenNetworkGalleryProperties : ClientProperties {
    override var clients: List<NetworkClientConfig> =
        listOf(
            object : NetworkClientConfig() {
                override val nodeName: String = "alice"
                override var network: CordaRPCNetwork = CordaRPCNetwork.GBP
                @Value("\${gbp.alice.rpc.url}")
                override lateinit var nodeUrl: String
                @Value("\${gbp.alice.rpc.username}")
                override lateinit var nodeUsername: String
                @Value("\${gbp.alice.rpc.password}")
                override lateinit var nodePassword: String
            },
            object : NetworkClientConfig() {
                override val nodeName: String = "alice"
                override var network: CordaRPCNetwork = CordaRPCNetwork.CBDC
                @Value("\${cdbc.alice.rpc.url}")
                override lateinit var nodeUrl: String
                @Value("\${cbdc.alice.rpc.username}")
                override lateinit var nodeUsername: String
                @Value("\${cbdc.alice.rpc.password}")
                override lateinit var nodePassword: String
            }
        )
}