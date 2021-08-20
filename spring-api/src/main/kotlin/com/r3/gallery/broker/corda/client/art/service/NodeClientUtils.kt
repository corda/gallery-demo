package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.client.api.RpcConnectionTarget
import com.r3.gallery.broker.corda.client.config.NetworkClientConfig

/**
 * Returns RPCConnectionId filtered on Corda Network membership
 * @overload multiple networks
 */
internal fun List<NetworkClientConfig>.rpcIdsByNetwork(network: CordaRPCNetwork) : List<RpcConnectionTarget> =
    this.filter { it.network == network }
        .map { it.id }
internal fun  List<NetworkClientConfig>.rpcIdsByNetwork(network: List<CordaRPCNetwork>) : List<RpcConnectionTarget> =
    network.flatMap { currentNetwork -> this.rpcIdsByNetwork(currentNetwork) }

