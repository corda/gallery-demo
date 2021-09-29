package com.r3.gallery.broker.corda.rpc.service

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.RpcConnectionTarget
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowHandle
import net.corda.core.node.NodeInfo

interface ConnectionService {

    /** The Corda network which this connection service handles RPC towards. */
    var associatedNetwork: CordaRPCNetwork

    /**
     * Stores all open/available CordaRpcConnections in the connection service. A session is created on first request
     * via [execute] or [startFlow] and will attempt to remain as needed.
     */
    var sessions: MutableMap<UniqueIdentifier, CordaRPCConnection>

    /**
     * Returns CordaRPCConnection for all available clients
     *
     * @return [Map] of proxies for all sessions open keyed by CordaX500Name
     */
    fun allProxies(): Map<CordaX500Name, Pair<CordaRPCNetwork, CordaRPCOps>>?

    /**
     * Returns any available active proxy or null
     *
     * @return [CordaRPCOps] if ANY session to any node available on connection service exists.
     */
    fun anyProxy(): CordaRPCOps?

    /**
     * Return proxy for network party
     *
     * @param networkParty to fetch proxy for
     * @return [CordaRPCOps]
     */
    fun proxyForParty(networkParty: String): CordaRPCOps

    /**
     * Returns all live target connections to node based on destination
     *
     * @return [Map] of all sessions which resolve to the implicit [RpcConnectionTarget]
     */
    fun RpcConnectionTarget.sessions(): Map<UniqueIdentifier, CordaRPCConnection>

    /**
     * Simple shorthand for describing connection id in terms of node vs network
     *
     * @param network the String value matching against a [CordaRPCNetwork] enum.
     */
    infix fun String.idOn(network: String) : RpcConnectionTarget

    /**
     * Returns NodeInfos for all configured nodes on this connection service.
     *
     * @param networks optional list of networks to filter on
     * @param dev default = false; rather than utilizing network map fetches across individual connections
     * @return [List][NodeInfo]
     */
    fun getNodes(networks: List<CordaRPCNetwork>? = null, dev: Boolean = false) : List<NodeInfo>

    /**
     * Optional initialization invoked when a connection is made to a node.
     *
     * @param node [RpcConnectionTarget] of the node which has just connected
     */
    fun initializeNodeService(node: RpcConnectionTarget) {}

    // extension function to disconnect a session and remove from sessions list
    fun UniqueIdentifier.disconnect()

    /**
     * Executes the RPC command against a target connection using existing connection if available otherwise executing
     * on a new connection.
     *
     * @param target the [RpcConnectionTarget] whose rpc session will run the flow.
     * @param block representing the proxy call to execute.
     * @return [A] return type of Flow being called by block.
     */
    fun <A> execute(target: RpcConnectionTarget, block: (CordaRPCConnection) -> A): A

    /**
     * Starts a flow via rpc against a target using [execute] as a connection handling wrapper
     *
     * @param networkParty x500 whose rpc connection will run the flow
     * @param logicType the flow class to run
     * @param args arguments to pass to logicType
     * @return [FlowHandle] with identifier and future for the operation.
     */
    fun <T> startFlow(networkParty: String, logicType: Class<out FlowLogic<T>>, vararg args: Any?): FlowHandle<T>

    /**
     * Simple proxy query to return a [Party] via proxy a, against name b.
     *
     * @param networkParty the x500 name of the proxy to request via
     * @param name the x500 to retrieve party object from
     * @return [Party]
     */
    fun wellKnownPartyFromName(networkParty: String, name: String): Party?
}

