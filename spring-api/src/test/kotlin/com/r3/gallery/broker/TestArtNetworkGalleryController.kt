package com.r3.gallery.broker

import com.nhaarman.mockito_kotlin.mock
import com.r3.gallery.broker.corda.client.api.ArtworkId
import com.r3.gallery.broker.corda.client.api.CordaRPCNetwork
import com.r3.gallery.broker.corda.client.art.controllers.ArtNetworkGalleryController
import com.r3.gallery.broker.corda.client.art.service.ArtNetworkGalleryClientImpl
import com.r3.gallery.broker.corda.client.config.ClientProperties
import com.r3.gallery.states.AuctionState
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

/**
 * CordaRPCOps mock based e2e - controller, service, mock
 */
@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [
    ArtNetworkGalleryController::class,
    ArtNetworkGalleryClientImpl::class,
    ClientProperties::class
])
class TestArtNetworkGalleryController {

    @Autowired
    lateinit var galleryClientImpl: ArtNetworkGalleryClientImpl

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `list available artwork`() {
        val states = mutableListOf(
            AuctionState(listOf(mock()), UniqueIdentifier(), UUID.randomUUID()),
            AuctionState(listOf(mock()), UniqueIdentifier(), UUID.randomUUID()),
        )
        val connection = createConnection()
        val rpcConnectionId = "O=Alice,L=London,C=GB"+CordaRPCNetwork.AUCTION.toString()

        injectStates(connection, states) // 2 art states
        injectConnections(mapOf(rpcConnectionId to connection), galleryClientImpl)

        val result = webTestClient.get().uri {
            it.path("/gallery/list-available-artworks")
                .queryParam("galleryParty","O=Alice,L=London,C=GB")
                .build()
        }.exchange()
            .expectStatus().isOk
            .expectBodyList(ArtworkId::class.java)
            .returnResult()

        assert(result.responseBody?.first() is UUID)
        assert(result.responseBody?.size == 2)
    }
}