package com.r3.gallery.broker

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.r3.gallery.broker.corda.client.api.RPCConnectionId
import com.r3.gallery.broker.corda.client.art.service.NodeClient
import com.r3.gallery.states.AuctionState
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.generateKeyPair
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import org.mockito.Mockito
import java.time.Instant


/**
 * Create a mock RPC connection and proxy
 */
fun createConnection(): CordaRPCConnection {
    val connection: CordaRPCConnection = mock()
    val proxy: CordaRPCOps = mock()

    whenever(connection.proxy).thenReturn(proxy)

    return connection
}

/**
 * Inject mock connections to <out NodeClient> map
 */
fun injectConnections(connections: Map<RPCConnectionId, CordaRPCConnection?> , nodeClient: NodeClient) {
    val rpcIdToCordaRPCClientsMapField = NodeClient::class.java
        .getDeclaredField("rpcIdToCordaRPCClientsMap")
        .apply { isAccessible = true }
    val connectionMapField = NodeClient::class.java
        .getDeclaredField("connections")
        .apply { isAccessible = true }

    rpcIdToCordaRPCClientsMapField.set(nodeClient, mapOf(connections.entries.first().key to mock<CordaRPCClient>()))
    connectionMapField.set(nodeClient, connections)

    val nodeInfo: NodeInfo = mock()
    val name = CordaX500Name.parse("O=Alice,L=London,C=GB")
    val party = Party(name, generateKeyPair().public)
    val connection = connections.entries.first().value!!

    whenever(nodeInfo.legalIdentities).thenReturn(listOf(party))
    whenever(connection.proxy.nodeInfo()).thenReturn(nodeInfo)
    whenever(connection.proxy.wellKnownPartyFromAnonymous(any())).thenAnswer{ mock ->
        mock.arguments[0] as Party
    }
    whenever(connection.proxy.wellKnownPartyFromX500Name(any())).thenAnswer{ mock ->
        Party(mock.arguments[0] as CordaX500Name, generateKeyPair().public)
    }
}

/**
 * Inject states into a mock connection and implement a mock
 * [CordaRPCOps.vaultQueryBy] implementation to return the states.
 */
fun injectStates(
    connection: CordaRPCConnection,
    states: List<AuctionState>
) {
    whenever(
        connection.proxy.vaultQuery(
            any<Class<AuctionState>>()
        )
    ).thenAnswer {
        mock ->
        val clazz = mock.arguments[0]
        val classes = states.filter { state -> state.javaClass == clazz }

        val list: List<AuctionState> = classes

        createVaultPage(list, list.size.toLong())
    }
    whenever(
        connection.proxy.vaultQueryBy(
            any<QueryCriteria>(),
            any<PageSpecification>(),
            any<Sort>(),
            any<Class<LinearState>>()
        )
    ).thenAnswer { mock ->
        val clazz = mock.arguments[3] as Class<*>
        val page = mock.arguments[1] as PageSpecification
        val classes = states.filter { state -> state.javaClass == clazz }

        val list: List<LinearState> = classes

        val first = Integer.max(0, (page.pageNumber - 1) * page.pageSize)
        val last = Integer.min(list.size, first + page.pageSize)

        val paged = if (first >= list.size) emptyList() else list.subList(first, last)

        createVaultPage(paged, list.size.toLong())
    }
}

/**
 * Create a mock vault consisting of the [linearStates], reporting
 * that [totalStates] are available.
 */
fun createVaultPage(
    linearStates: List<LinearState>,
    totalStates: Long = linearStates.size.toLong(),
    status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED
): Vault.Page<LinearState> {
    val states = linearStates.map { linearState ->
        val state: StateAndRef<LinearState> = mock()
        val txState: TransactionState<LinearState> = mock()
        whenever(state.state).thenReturn(txState)
        whenever(txState.data).thenReturn(linearState)

        state
    }
    val metadata = linearStates.map { _ ->
        val meta: Vault.StateMetadata = mock()
        whenever(meta.recordedTime).thenReturn(Instant.now())

        meta
    }

    return Vault.Page(
        states,
        metadata,
        totalStates,
        status,
        emptyList()
    )
}

/**
 * Mock helper that allows class checks to run on non-nullable fields
 */
private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

@Suppress("unchecked_cast")
private fun <T> uninitialized(): T = null as T