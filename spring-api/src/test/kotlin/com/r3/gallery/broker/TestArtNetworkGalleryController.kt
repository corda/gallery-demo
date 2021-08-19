package com.r3.gallery.broker

import com.r3.gallery.broker.corda.client.api.ArtworkId
import com.r3.gallery.broker.corda.client.art.controllers.ArtNetworkGalleryController
import com.r3.gallery.broker.corda.client.art.service.ArtNetworkGalleryClientImpl
import com.r3.gallery.broker.corda.client.config.ClientProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*


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
    lateinit var galleryController: ArtNetworkGalleryController

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `test live auctions`() {
        val result = webTestClient.get().uri {
            it.path("/gallery/list-available-artworks")
                .queryParam("galleryParty","test")
                .build()
        }.exchange()
            .expectStatus().isOk
            .expectBodyList(ArtworkId::class.java)
            .returnResult()

        assert(result.responseBody?.first() is UUID)
    }
}