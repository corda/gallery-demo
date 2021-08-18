package com.r3.gallery.broker.corda.client.config

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Connection and credential properties for Bidders
 */

@Component("ArtNetworkBidderProperties")
class ArtNetworkBidderProperties : ClientProperties {
    override var clients: List<NetworkClientConfig> =
        listOf(
            object : NetworkClientConfig() {
                override var nodeName: String = "bob"
                override var network: CordaRPCNetwork = CordaRPCNetwork.AUCTION
                @Value("\${auction.bob.rpc.url}")
                override lateinit var nodeUrl: String
                @Value("\${auction.bob.rpc.username}")
                override lateinit var nodeUsername: String
                @Value("\${auction.bob.rpc.password}")
                override lateinit var nodePassword: String
            },
            object : NetworkClientConfig() {
                override var nodeName: String = "charlie"
                override var network: CordaRPCNetwork = CordaRPCNetwork.AUCTION
                @Value("\${auction.charlie.rpc.url}")
                override lateinit var nodeUrl: String
                @Value("\${auction.charlie.rpc.username}")
                override lateinit var nodeUsername: String
                @Value("\${auction.charlie.rpc.password}")
                override lateinit var nodePassword: String
            }
        )
}

@Component("TokenNetworkBidderProperties")
class TokenNetworkBidderProperties : ClientProperties {
    override var clients: List<NetworkClientConfig> =
        listOf(
            object : NetworkClientConfig() {
                override val nodeName: String = "bob"
                override var network: CordaRPCNetwork = CordaRPCNetwork.GBP
                @Value("\${gbp.bob.rpc.url}")
                override lateinit var nodeUrl: String
                @Value("\${gbp.bob.rpc.username}")
                override lateinit var nodeUsername: String
                @Value("\${gbp.bob.rpc.password}")
                override lateinit var nodePassword: String
            },
            object : NetworkClientConfig() {
                override val nodeName: String = "charlie"
                override var network: CordaRPCNetwork = CordaRPCNetwork.CBDC
                @Value("\${cbdc.charlie.rpc.url}")
                override lateinit var nodeUrl: String
                @Value("\${cbdc.charlie.rpc.username}")
                override lateinit var nodeUsername: String
                @Value("\${cbdc.charlie.rpc.password}")
                override lateinit var nodePassword: String
            }
        )
}