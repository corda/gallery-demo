package com.r3.gallery.broker.corda.rpc.service

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.RpcConnectionTarget
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import net.corda.client.rpc.*
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowHandle
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

/**
 * Generic class for handling RPCClient connections and node interactions
 */
open class ConnectionServiceImpl(
    /** The properties which configure this connection instance */
    private val clientProperties: ClientProperties,
    /** The Corda network which this connection service handles RPC towards. */
    override var associatedNetwork: CordaRPCNetwork
) : ConnectionService {

    companion object {
        private val logger = LoggerFactory.getLogger(ConnectionServiceImpl::class.java)

        private const val MINIMUM_SERVER_PROTOCOL_VERSION = 4
        const val TIMEOUT = 180L
    }

    /**
     * Clients mapped from configurations - populated at init.
     */
    private var rpcTargetToCordaRpcClientsMap: Map<RpcConnectionTarget, CordaRPCClient>? = null

    /**
     * Stores all open/available CordaRpcConnections in the connection service. A session is created on first request
     * via [execute] or [startFlow] and will attempt to remain as needed.
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
     * Returns CordaRPCConnection for all available clients
     *
     * @return [Map] of proxies for all sessions open keyed by CordaX500Name
     */
    override fun allProxies(): Map<CordaX500Name, Pair<CordaRPCNetwork, CordaRPCOps>>? {
        return rpcTargetToCordaRpcClientsMap?.keys?.associate {
            val x500 = CordaX500Name.parse(partyFromConnectionTarget(it))
            val sessionId = it.connect()
            Pair(x500, Pair(associatedNetwork, sessions[sessionId]!!.proxy))
        }
    }

    /**
     * Returns any available active proxy or null
     *
     * @return [CordaRPCOps] if ANY session to any node available on connection service exists.
     */
    override fun anyProxy(): CordaRPCOps? {
        return sessions.values.firstOrNull()?.proxy
    }

    /**
     * Return proxy for network party
     *
     * @param networkParty to fetch proxy for
     * @return [CordaRPCOps]
     */
    override fun proxyForParty(networkParty: String): CordaRPCOps {
        val sessionId = connectionTargetFromParty(networkParty).connect()
        return sessions[sessionId]?.proxy ?: throw IllegalArgumentException("No proxy found for $networkParty on $associatedNetwork")
    }

    /**
     * Returns all live target connections to node based on destination
     *
     * @return [Map] of all sessions which resolve to the implicit [RpcConnectionTarget]
     */
    override fun RpcConnectionTarget.sessions(): Map<UniqueIdentifier, CordaRPCConnection> {
        return sessions.filterKeys { it.externalId == this }
    }

    /**
     * Returns connection for a single session based on it's key.
     * Note: possibly null due to onDisconnect hook from serverside disconnect
     *
     * @return [CordaRPCConnection]
     */
    private fun UniqueIdentifier.session(): CordaRPCConnection? {
        return sessions[this]
    }

    /**
     * Attempts to establish a CordaRPCConnection. Will retry 5 times, otherwise throw exception. After establishing
     * a connection adds to sessions map.
     *
     * @param target to establish connection to.
     * @return [UniqueIdentifier] representing the key of the new connection stored in sessions map.
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

    /** extension function variation that returns a given connection if exists otherwise creates a new connection */
    @JvmName("connectExtension")
    private fun RpcConnectionTarget.connect() : UniqueIdentifier {
        return sessions.keys.firstOrNull { it.externalId == this } ?: connect(this)
    }

    /** extension function to disconnect a session and remove from sessions list */
    override fun UniqueIdentifier.disconnect() {
        this.session()?.notifyServerAndClose()
        sessions.remove(this)
    }

    /**
     * Checks if a proposed rpcConnectionId exists based on network configurations
     *
     * @param rpcConnectionTarget to check if this connection target has been loaded into service through configuration.
     */
    private fun idExists(rpcConnectionTarget: RpcConnectionTarget)
        = require(rpcTargetToCordaRpcClientsMap!!.containsKey(rpcConnectionTarget))

    /**
     * Simple shorthand for describing connection id in terms of node vs network
     *
     * @param network the String value matching against a [CordaRPCNetwork] enum.
     */
    override infix fun String.idOn(network: String) : RpcConnectionTarget {
        val id = this + network.toUpperCase()
        idExists(id)
        return id
    }

    /**
     * Returns NodeInfos for all configured nodes on this connection service.
     *
     * @param networks optional list of networks to filter on
     * @param dev default = false; rather than utilizing network map fetches across individual connections
     * @return [List][NodeInfo]
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
     *
     * @param node [RpcConnectionTarget] of the node which has just connected
     */
    override fun initializeNodeService(node: RpcConnectionTarget) {
        logger.info("Starting node service $node")
    }

    /**
     * Executes the RPC command against a target connection using existing connection if available otherwise executing
     * on a new connection.
     *
     * @param target the [RpcConnectionTarget] whose rpc session will run the flow.
     * @param block representing the proxy call to execute.
     * @return [A] return type of Flow being called by block.
     */
    override fun <A> execute(target: RpcConnectionTarget, block: (CordaRPCConnection) -> A): A {
        val sessionId = target.connect() // open
        return block(sessions[sessionId]!!) // execute
    }

    /**
     * Starts a flow via rpc against a target using [execute] as a connection handling wrapper
     *
     * @param networkParty x500 whose rpc connection will run the flow
     * @param logicType the flow class to run
     * @param args arguments to pass to logicType
     * @return [FlowHandle] with identifier and future for the operation.
     */
    override fun <T> startFlow(networkParty: String, logicType: Class<out FlowLogic<T>>, vararg args: Any?): FlowHandle<T> {
        return execute(connectionTargetFromParty(networkParty)) { connections ->
            connections.proxy.startFlowDynamic(
                logicType,
                *args
            )
        }
    }

    /**
     * Simple shorthand for describing connection id in terms of node vs network
     *
     * @param networkParty to generate return from.
     * @return [RpcConnectionTarget] and checks the target exists through the client list
     */
    private fun connectionTargetFromParty(networkParty: String): RpcConnectionTarget {
        return associatedNetwork.let {
            (networkParty + associatedNetwork.toString())
                .also { idExists(it) } // check validity
        }
    }

    /**
     * Removes the associated network from a RPCConnectionTarget string returning the X500 base.
     *
     * @param connectionTarget to extract x500 from
     * @return x500 string
     */
    private fun partyFromConnectionTarget(connectionTarget: RpcConnectionTarget): String {
        return connectionTarget.removeSuffix(associatedNetwork.toString())
    }

    /**
     * Simple proxy query to return a [Party] via proxy a, against name b.
     *
     * @param networkParty the x500 name of the proxy to request via
     * @param name the x500 to retrieve party object from
     * @return [Party]
     */
    override fun wellKnownPartyFromName(networkParty: String, name: String): Party? {
        return execute(connectionTargetFromParty(networkParty)) { connections ->
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

/**
 * Accessible connection service definitions for access to a particular network.
 */
@Component
class AuctionConnectionService(
    @Autowired
    @Qualifier("AuctionNetworkProperties")
    auctionNetworkProperties: ClientProperties
) : ConnectionServiceImpl(auctionNetworkProperties, CordaRPCNetwork.AUCTION)

@Component
class GBPConnectionService(
    @Autowired
    @Qualifier("GbpNetworkProperties")
    gbpNetworkProperties: ClientProperties
) : ConnectionServiceImpl(gbpNetworkProperties, CordaRPCNetwork.GBP)

@Component
class CBDCConnectionService(
    @Autowired
    @Qualifier("CbdcNetworkProperties")
    cbdcNetworkProperties: ClientProperties
) : ConnectionServiceImpl(cbdcNetworkProperties, CordaRPCNetwork.CBDC)

/**
 * A wrapper class to access connection services across multiple networks.
 */
@Component
class ConnectionManager(
    @Autowired
    val auction: AuctionConnectionService,
    @Autowired
    val gbp: GBPConnectionService,
    @Autowired
    val cbdc: CBDCConnectionService
) {

    /**
     * Returns a [ConnectionService] corresponding to a [CordaRPCNetwork]
     *
     * @param network to fetch connection service for.
     */
    fun getCSbyNetwork(network: CordaRPCNetwork): ConnectionService {
        when (network.name) {
            "AUCTION" -> { return auction }
            "GBP" -> { return gbp }
            "CBDC" -> { return cbdc }
            else -> throw IllegalArgumentException("Connection Service for network $network not found!")
        }
    }
}