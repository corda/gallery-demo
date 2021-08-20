package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.client.api.RpcConnectionTarget
import com.r3.gallery.broker.corda.client.config.ClientProperties
import net.corda.client.rpc.*
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

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
    private var rpcTargetToCordaRpcClientsMap: Map<RpcConnectionTarget, CordaRPCClient>? = null

    /**
     * Store sessions: UniqueIdentifier [UUID, RpcConnectionTarget] / ConnectionInstance
     * Each request will open-close new unique session
     */
    private var sessions: MutableMap<UniqueIdentifier, CordaRPCConnection> = ConcurrentHashMap()

    init {
        // Establish clients for all available destinations
        rpcTargetToCordaRpcClientsMap = clientProperties.clients.associate {
            val currentClient = CordaRPCClient(
                NetworkHostAndPort.parse(it.nodeUrl),
                CordaRPCClientConfiguration.DEFAULT.copy(minimumServerProtocolVersion = MINIMUM_SERVER_PROTOCOL_VERSION)
            )
            Pair(it.id, currentClient)
        }
    }

    /**
     * Returns all live target connections to node based on destination
     */
    private fun RpcConnectionTarget.sessions(): Map<UniqueIdentifier, CordaRPCConnection> {
        return sessions.filterKeys { it.externalId == this }
    }

    /**
     * Returns connection for a single session
     * - possibly null do to onDisconnect hook from serverside disconnect
     */
    private fun UniqueIdentifier.session(): CordaRPCConnection? {
        return sessions[this]
    }

    /**
     * CordaRPCConnection logic establishes new unique connection
     */
    private fun connect(target: RpcConnectionTarget) : UniqueIdentifier {
        val nodeProperties = clientProperties.getConfigById(target)!!
        var retries = 0
        val sessionId = UniqueIdentifier(externalId = target) //  assign destination  to external
        var sessionConnection: CordaRPCConnection? = null
        while (retries < 5) { // max retries
            try {
                val targetClient: CordaRPCClient = rpcTargetToCordaRpcClientsMap!![target]!!
                sessionConnection = targetClient.start(
                    nodeProperties.nodeUsername,
                    nodeProperties.nodePassword,
                    GracefulReconnect(onDisconnect = { sessions.remove(sessionId) })
                )
                break
            } catch (e: RPCException) {
                Thread.sleep(5000)
                retries++
            }
        }
        sessionConnection?.let {
            sessions[sessionId] = sessionConnection
            initializeNodeService(target) // post-process
            return sessionId
        }
        throw RPCException("Unable to establish connection to $target")
    }
    // extension function variation
    @JvmName("connectExtension")
    private fun RpcConnectionTarget.connect() : UniqueIdentifier = connect(this)
    private fun UniqueIdentifier.disconnect() {
        this.session()?.close()
        sessions.remove(this)
    }

    /**
     * Simple shorthand for describing connection id in terms of node vs network
     */
    protected infix fun String.idOn(network: String) : RpcConnectionTarget {
        val id = this + network.toUpperCase()
        require(rpcTargetToCordaRpcClientsMap!!.containsKey(id))
        return id
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
            clientProperties.clients.rpcIdsByNetwork(networks)
        } else {
            rpcTargetToCordaRpcClientsMap!!.keys
        }

        // dev-mode per connection fetch (tests connections at same time)
        return if (dev) {
            targetRpcIds.map {
                execute(it) { connection ->
                    connection.proxy.nodeInfo()
                }
            }
        } else { // single connection via network map
            execute(rpcTargetToCordaRpcClientsMap!!.keys.first()) { connection ->
                connection.proxy.networkMapSnapshot()
            }
        }
    }

    /**
     * Optional initialization invoked when a connection is made to a node.
     * @param node [RpcConnectionTarget] of the node which has just connected
     */
    open fun initializeNodeService(node: RpcConnectionTarget) {
        logger.info("Starting node service $node")
    }

    /**
     * Executes the RPC command against a target connection
     */
    protected fun <A> execute(target: RpcConnectionTarget, block: (CordaRPCConnection) -> A): A {
        val sessionId = target.connect() // open
        val result = block(sessions[sessionId]!!) // execute
        sessionId.disconnect() // close
        return result
    }

    /**
     * Starts a flow on the given RPC connection
     */
    protected fun <T> RPCConnectionTarget.startFlow(logicType: Class<out FlowLogic<T>>, vararg args: Any?): T {
        return execute(this) { connections ->
            connections.proxy.startFlowDynamic(
                logicType,
                *args
            )
        }.returnValue.get(TIMEOUT, TimeUnit.SECONDS)
    }
}
