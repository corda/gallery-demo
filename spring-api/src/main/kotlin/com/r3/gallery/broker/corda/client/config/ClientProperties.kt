package com.r3.gallery.broker.corda.client.config

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.client.api.RPCConnectionId

/**
 * API for logical functional grouping of node configurations
 * Used to segregate configurations along Bidder/Gallery roles
 */
interface ClientProperties {
    var clients: List<NetworkClientConfig>

    /**
     * Returns NetworkClientConfig matching id (unique across networks)
     * @param id of target node/network config
     */
    fun getConfigById(id: RPCConnectionId) : NetworkClientConfig? =
        clients.first {
            it.id == id
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