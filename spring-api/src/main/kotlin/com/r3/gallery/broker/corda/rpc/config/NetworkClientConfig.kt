package com.r3.gallery.broker.corda.rpc.config

import com.r3.gallery.api.CordaRPCNetwork
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * A configuration class to be instantiated inline with injectables. Required for Corda RPC Connections
 */
data class NetworkClientConfig(
    val nodeName: String,
    val network: CordaRPCNetwork,
    val nodeUrl: String,
    val nodeUsername: String,
    val nodePassword: String
) {
    val id: String
        get() = nodeName+network
}

/**
 * Data model for storing applications.properties values defined under `rpc` key.
 */
@ConstructorBinding
@ConfigurationProperties("rpc")
data class RpcProperties(
    var aliceX500: String,
    var bobX500: String,
    var charlieX500: String,
    var username: String,
    var password: String,
    val auction: AuctionNet,
    val gbp: GbpNet,
    val cbdc: CbdcNet
) {
    data class AuctionNet(val url: AuctionURL) {
        data class AuctionURL(
            var alice: String,
            var bob: String,
            var charlie: String
        )
    }
    data class GbpNet(val url: GbpURL) {
        data class GbpURL(
            var alice: String,
            var bob: String
        )
    }
    data class CbdcNet(val url: CbdcURL) {
        data class CbdcURL(
            var alice: String,
            var charlie: String
        )
    }
}