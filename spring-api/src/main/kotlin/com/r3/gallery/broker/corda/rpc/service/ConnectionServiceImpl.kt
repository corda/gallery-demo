package com.r3.gallery.broker.corda.rpc.service

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.RpcConnectionTarget
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import net.corda.client.rpc.*
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy

/**
 * Generic class for handling RPCClient connections and node interactions
 */
class ConnectionServiceImpl(private val clientProperties: ClientProperties) : ConnectionService {

    companion object {
        private val logger = LoggerFactory.getLogger(ConnectionServiceImpl::class.java)

        private const val MINIMUM_SERVER_PROTOCOL_VERSION = 4
        const val TIMEOUT = 30L
    }

    /**
     * Corda network which this connection service handles RPC towards.
     * - Should set as quickly as possible after init as needed to resolve rpcConnectionTargets
     */
    override var associatedNetwork: CordaRPCNetwork? = null

    /**
     * Clients mapped from configurations
     */
    private var rpcTargetToCordaRpcClientsMap: Map<RpcConnectionTarget, CordaRPCClient>? = null

    /**
     * Store sessions: UniqueIdentifier [UUID, RpcConnectionTarget] / Connection Instance
     * Each request will open-close new unique session
     */
    override var sessions: MutableMap<UniqueIdentifier, CordaRPCConnection> = ConcurrentHashMap()

    init {
        // Establish clients for all available destinations in client properties
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
    override fun RpcConnectionTarget.sessions(): Map<UniqueIdentifier, CordaRPCConnection> {
        return sessions.filterKeys { it.externalId == this }
    }

    /**
     * Returns connection for a single session
     * - possibly null due to onDisconnect hook from serverside disconnect
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

    // extension function variation returns a given connection if exists otherwise creates a new connection
    @JvmName("connectExtension")
    private fun RpcConnectionTarget.connect() : UniqueIdentifier {
        return sessions.keys.firstOrNull { it.externalId == this } ?: connect(this)
    }

    // extension function to disconnect a session and remove from sessions list
    override fun UniqueIdentifier.disconnect() {
        this.session()?.notifyServerAndClose()
        sessions.remove(this)
    }

    /**
     * Checks if a proposed rpcConnectionId exists based on network configurations
     */
    private fun idExists(rpcConnectionTarget: RpcConnectionTarget)
        = require(rpcTargetToCordaRpcClientsMap!!.containsKey(rpcConnectionTarget))

    /**
     * Simple shorthand for describing connection id in terms of node vs network
     */
    override infix fun String.idOn(network: String) : RpcConnectionTarget {
        val id = this + network.toUpperCase()
        idExists(id)
        return id
    }

    /**
     * Returns NodeInfos for all configured nodes.
     *
     * @param networks optional list of networks to filter on
     * @param dev default = false; rather than utilizing network map fetches across individual connections
     */
    override fun getNodes(networks: List<CordaRPCNetwork>?, dev: Boolean) : List<NodeInfo> {
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
    override fun initializeNodeService(node: RpcConnectionTarget) {
        logger.info("Starting node service $node")
    }

    /**
     * Executes the RPC command against a target connection using existing connection if avail
     */
    override fun <A> execute(target: RpcConnectionTarget, block: (CordaRPCConnection) -> A): A {
        val sessionId = target.connect() // open
        return block(sessions[sessionId]!!) // execute
    }

    /**
     * Starts a flow via rpc against a target
     */
    override fun <T> startFlow(networkParty: String, logicType: Class<out FlowLogic<T>>, vararg args: Any?): T {
        return execute(getConnectionTarget(networkParty)) { connections ->
            connections.proxy.startFlowDynamic(
                logicType,
                *args
            )
        }.returnValue.get(TIMEOUT, TimeUnit.SECONDS)
    }

    /**
     * Simple shorthand for describing connection id in terms of node vs network
     * @return RpcConnectionTarget and checks the target exists through the client list
     */
    private fun getConnectionTarget(networkParty: String): RpcConnectionTarget {
        return associatedNetwork?.let {
            (networkParty + associatedNetwork.toString())
                .also { idExists(it) } // check validity
        } ?: throw IllegalStateException("Cannot target a rpc connection without setting the associatedNetwork of ${this::class.simpleName}")
    }

   override fun wellKnownPartyFromName(networkParty: String, name: String): Party? {
        return execute(getConnectionTarget(networkParty)) { connections ->
            connections.proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(name))
        }
    }

    /**
     * Function that runs as cleanup
     * It is used to notify each corda node about the connection's termination
     */
    @PreDestroy
    private fun closeConnections() {
        sessions.keys.forEach {
            it.disconnect()
        }
    }
}

