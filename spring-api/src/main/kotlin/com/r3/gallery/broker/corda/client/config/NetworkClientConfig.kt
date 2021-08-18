package com.r3.gallery.broker.corda.client.config

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import org.springframework.stereotype.Component

/**
 * A configuration class to be instantiated inline with injectables
 * required for Corda RPC Connections
 */
@Component
abstract class NetworkClientConfig {
    val id: String by lazy { nodeName+network }
    abstract val nodeName: String
    abstract val network: CordaRPCNetwork
    abstract var nodeUrl: String
    abstract var nodeUsername: String
    abstract var nodePassword: String
}