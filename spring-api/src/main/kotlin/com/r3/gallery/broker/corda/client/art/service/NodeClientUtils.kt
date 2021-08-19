package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.client.api.RPCConnectionId
import com.r3.gallery.broker.corda.client.config.NetworkClientConfig

/**
 * Returns RPCConnectionId filtered on Corda Network membership
 * @overload multiple networks
 */
internal fun List<NetworkClientConfig>.rpcIdsByNetwork(network: CordaRPCNetwork) : List<RPCConnectionId> =
    this.filter { it.network == network }
        .map { it.id }
internal fun  List<NetworkClientConfig>.rpcIdsByNetwork(network: List<CordaRPCNetwork>) : List<RPCConnectionId> =
    network.flatMap { currentNetwork -> this.rpcIdsByNetwork(currentNetwork) }

