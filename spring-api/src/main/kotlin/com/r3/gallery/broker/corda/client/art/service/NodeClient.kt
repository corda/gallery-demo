package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.RPCConnectionId
import com.r3.gallery.broker.corda.client.config.ClientProperties
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCClientConfiguration
import net.corda.client.rpc.CordaRPCConnection
import net.corda.client.rpc.GracefulReconnect
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

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
     * Clients mapped from configurations
     */
    private var rpcIdToCordaRPCClientsMap: Map<RPCConnectionId, CordaRPCClient>? = null

    /**
     * Store connections via client id
     */
    private var connections: MutableMap<RPCConnectionId, CordaRPCConnection?>? = null

    init {
        rpcIdToCordaRPCClientsMap = clientProperties.clients.associate {
            val currentClient = CordaRPCClient(
                NetworkHostAndPort.parse(it.nodeUrl),
                CordaRPCClientConfiguration.DEFAULT.copy(minimumServerProtocolVersion = MINIMUM_SERVER_PROTOCOL_VERSION)
            )
            Pair(it.id, currentClient)
        }
        connections = clientProperties.clients.associate {
            Pair(it.id, null)
        }.toMutableMap()
    }

    /**
     * Returns a target connection to node or creates if not existing
     */
    private fun RPCConnectionId.connection(): CordaRPCConnection {
        if (connections!![this] == null) {
            this.connect()
        }

        return connections!![this]!!
    }

    /**
     * CordaRPCConnection logic with post-initialization logic
     */
    private fun RPCConnectionId.connect() {
        val nodeProperties = clientProperties.getConfigById(this)!!
        connections!![this] = rpcIdToCordaRPCClientsMap!![this]?.start(
            nodeProperties.nodeUsername,
            nodeProperties.nodePassword,
            GracefulReconnect(onDisconnect = { connections!![this] = null })
        )

        initializeNodeService(this)
    }

    /**
     * Checks if a proposed rpcConnectionId exists based on network configurations
     */
    protected fun idExists(rpcConnectionId: RPCConnectionId)
        = require(rpcIdToCordaRPCClientsMap!!.containsKey(rpcConnectionId))

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
            clientProperties.clients.rpcIdsByNetwork(networks)
        } else {
            rpcIdToCordaRPCClientsMap!!.keys
        }

        // dev-mode per connection fetch (tests connections at same time)
        return if (dev) {
            targetRpcIds.map {
                execute(it) { connection ->
                    connection.proxy.nodeInfo()
                }
            }
        } else { // single connection via network map
            execute(rpcIdToCordaRPCClientsMap!!.keys.first()) { connection ->
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
    private fun <A> execute(target: RPCConnectionId, block: (CordaRPCConnection) -> A): A {
        return block(target.connection())
    }

    /**
     * Starts a flow on the given RPC connection
     */
    protected fun <T> RPCConnectionId.startFlow(logicType: Class<out FlowLogic<T>>, vararg args: Any?): T {
        return execute(this) { connections ->
            connections.proxy.startFlowDynamic(
                logicType,
                *args
            )
        }.returnValue.get(TIMEOUT, TimeUnit.SECONDS)
    }

    fun RPCConnectionId.wellKnownPartyFromName(name: String): Party {
        return execute(this) { connections ->
            connections.proxy.partiesFromName(name, true).first()
        }
    }
}
