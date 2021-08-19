package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.client.api.RPCConnectionId
import com.r3.gallery.broker.corda.client.config.NetworkClientConfig

/**
 * Returns RPCConnectionId filtered on Corda Network membership
 * @overload multiple networks
 */
internal fun rpcIdsByNetwork(clients: List<NetworkClientConfig>, network: CordaRPCNetwork) : List<RPCConnectionId> =
    clients.filter { it.network == network }
        .map { it.id }
internal fun  rpcIdsByNetwork(clients: List<NetworkClientConfig>, network: List<CordaRPCNetwork>) : List<RPCConnectionId> =
    network.flatMap { currentNetwork -> rpcIdsByNetwork(clients, currentNetwork) }
