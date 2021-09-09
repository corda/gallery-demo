package com.r3.gallery.broker.corda.rpc.service

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.RpcConnectionTarget
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.node.NodeInfo

interface ConnectionService {

    // Corda network which this connection service handles RPC towards.
    var associatedNetwork: CordaRPCNetwork?

    /**
     * Store sessions: UniqueIdentifier [UUID, RpcConnectionTarget] / Connection Instance
     * Each request will open-close new unique session
     */
    var sessions: MutableMap<UniqueIdentifier, CordaRPCConnection>

    /**
     * Returns CordaRPCConnection for all available clients
     */
    fun allConnections(): List<CordaRPCConnection>?

    /**
     * Returns all live target connections to node based on destination
     */
    fun RpcConnectionTarget.sessions(): Map<UniqueIdentifier, CordaRPCConnection>

    /**
     * Simple shorthand for describing connection id in terms of node vs network
     */
    infix fun String.idOn(network: String) : RpcConnectionTarget

    /**
     * Returns NodeInfos for all configured nodes.
     *
     * @param networks optional list of networks to filter on
     * @param dev default = false; rather than utilizing network map fetches across individual connections
     */
    fun getNodes(networks: List<CordaRPCNetwork>? = null, dev: Boolean = false) : List<NodeInfo>

    /**
     * Optional initialization invoked when a connection is made to a node.
     * @param node [RpcConnectionTarget] of the node which has just connected
     */
    fun initializeNodeService(node: RpcConnectionTarget) {}

    // extension function to disconnect a session and remove from sessions list
    fun UniqueIdentifier.disconnect()

    /**
     * Executes the RPC command against a target connection using existing connection if avail
     */
    fun <A> execute(target: RpcConnectionTarget, block: (CordaRPCConnection) -> A): A

    /**
     * Starts a flow via rpc against a target
     */
    fun <T> startFlow(networkParty: String, logicType: Class<out FlowLogic<T>>, vararg args: Any?): T
}