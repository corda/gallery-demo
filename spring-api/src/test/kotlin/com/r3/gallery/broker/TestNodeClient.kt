package com.r3.gallery.broker

import com.r3.gallery.broker.corda.client.art.service.NodeClient
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

//@ExtendWith(SpringExtension::class)
class TestNodeClient : TestArtNetworkGalleryController() {

    private lateinit var nodeClient: NodeClient

    /**
     * Checks the session list pre/post query. List should be empty as connections
     * stored only for duration of the request.
     */
    @Test
    fun `delegate test on rpc connection sessions should create and delete`() {
        nodeClient = galleryClientImpl

        val rpcSessionsField = NodeClient::class.java
            .getDeclaredField("sessions")
            .apply { isAccessible = true }

        assert(nodeClient.field(rpcSessionsField).isEmpty())

        `list available artwork`()

        assert(nodeClient.field(rpcSessionsField).isEmpty())
    }

    private fun NodeClient.field(field: Field) : Map<*,*> = field.get(this) as Map<*, *>

}