package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.client.api.RPCConnectionId
import com.r3.gallery.broker.corda.client.config.ClientProperties
import net.corda.client.rpc.*
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort
import org.slf4j.LoggerFactory

/**
 * Generic class for handling RPCClient connections and node interactions
 */
abstract class NodeClient(private val clientProperties: ClientProperties) {
    companion object {
        private val logger = LoggerFactory.getLogger(NodeClient::class.java)

        private const val MINIMUM_SERVER_PROTOCOL_VERSION = 4
        const val TIMEOUT = 30L
    }

    /**
     * Generate clients mapped from configurations
     */
    private val rpcIdToClientsMap: Map<RPCConnectionId, CordaRPCClient> by lazy {
        clientProperties.clients.associate {
            val currentClient = CordaRPCClient(
                NetworkHostAndPort.parse(it.nodeUrl),
                CordaRPCClientConfiguration.DEFAULT.copy(minimumServerProtocolVersion = MINIMUM_SERVER_PROTOCOL_VERSION)
            )
            Pair(it.id, currentClient)
        }
    }

    /**
     * Store connections via client id
     */
    private val connections: MutableMap<RPCConnectionId, CordaRPCConnection?> by lazy {
        clientProperties.clients.associate {
            Pair(it.id, null)
        }.toMutableMap()
    }

    /**
     * Returns a target connection to node or creates if not existing
     */
    private fun RPCConnectionId.connection(): CordaRPCConnection {
        if (connections[this] == null) {
            this.connect()
        }

        return connections[this]!!
    }

    /**
     * CordaRPCConnection logic with post-initialization logic
     */
    private fun RPCConnectionId.connect() {
        val nodeProperties = clientProperties.getConfigById(this)!!
        connections[this] = rpcIdToClientsMap[this]?.start(
            nodeProperties.nodeUsername,
            nodeProperties.nodePassword,
            GracefulReconnect(onDisconnect = { connections[this] = null })
        )

        initializeNodeService(this)
    }

    /**
     * Simple shorthand for describing connection id in terms of node vs network
     */
    protected infix fun String.idOn(network: String) : RPCConnectionId {
        val id = this + network
        require(rpcIdToClientsMap.containsKey(id))
        return id.toLowerCase()
    }

    /**
     * Returns NodeInfos for all configured nodes.
     *
     * @param networks optional list of networks to filter on
     * @param dev default = false; rather than utilizing network map fetches across
     * individual connections (useful for test).
     */
    fun getNodes(networks: List<CordaRPCNetwork>? = null, dev: Boolean = false) : List<NodeInfo> {
        // filter to networks if necessary
        val targetRpcIds = if (!networks.isNullOrEmpty()) {
            rpcIdsByNetwork(clientProperties.clients, networks)
        } else {
            rpcIdToClientsMap.keys
        }

        // dev-mode per connection fetch (tests connections at same time)
        return if (dev) {
            targetRpcIds.map {
                execute(it) { connection ->
                    connection.proxy.nodeInfo()
                }
            }
        } else { // single connection via network map
            execute(rpcIdToClientsMap.keys.first()) { connection ->
                connection.proxy.networkMapSnapshot()
            }
        }
    }

    /**
     * Optional initialization invoked when a connection is made to a node.
     * @param node [RPCConnectionId] of the node which has just connected
     */
    open fun initializeNodeService(node: RPCConnectionId) {
        logger.info("Starting node service $node")
    }

    /**
     * Executes the RPC command against a target connection
     */
    protected fun <A> execute(target: RPCConnectionId, block: (CordaRPCConnection) -> A): A {
        return block(target.connection())
    }
}
