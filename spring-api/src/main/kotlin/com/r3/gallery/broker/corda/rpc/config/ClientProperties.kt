package com.r3.gallery.broker.corda.rpc.config

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.RpcConnectionTarget

/**
 * API for logical functional grouping of node configurations
 * Used to segregate configurations along Bidder/Gallery roles
 */
interface ClientProperties {
    var clients: List<NetworkClientConfig>

    /**
     * Returns NetworkClientConfig matching id (unique across networks)
     * @param target of target node/network config
     */
    fun getConfigById(target: RpcConnectionTarget) : NetworkClientConfig? =
        clients.first {
            it.id == target
        }

    /**
     * Returns all NetworkClientConfigs across any networks matching nodeName
     * @param name of the node  e.g. 'alice'
     */
    fun getConfigsByNodeName(name: String) : List<NetworkClientConfig>? =
        clients.filter {
            it.nodeName == name
        }

    /**
     * Returns all configs via string identifier of a network
     * @param network e.g. 'auction'
     */
    fun getConfigsByNetwork(network: String) : List<NetworkClientConfig>? =
        clients.filter {
            it.network.name == network.toUpperCase()
        }

    /**
     * Overload for enum identifier of network
     * @param network
     */
    fun getConfigsByNetwork(network: CordaRPCNetwork) : List<NetworkClientConfig>? =
        clients.filter {
            it.network == network
        }
}