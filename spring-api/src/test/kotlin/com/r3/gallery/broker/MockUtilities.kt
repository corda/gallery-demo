package com.r3.gallery.broker

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import org.mockito.Mockito


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
fun injectConnections() {}

/**
 * Mock helper that allows class checks to run on non-nullable fields
 */
private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

@Suppress("unchecked_cast")
private fun <T> uninitialized(): T = null as T