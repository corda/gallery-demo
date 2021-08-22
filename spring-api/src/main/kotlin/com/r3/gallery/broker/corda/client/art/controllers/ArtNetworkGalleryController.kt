package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.art.service.NodeClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * TODO: switch returns to Mono/Flux reactive
 */
@CrossOrigin
@RestController
@RequestMapping("/gallery")
class ArtNetworkGalleryController(private val artNetworkGalleryClient: ArtNetworkGalleryClient) {
    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkGalleryController::class.java)
        const val TIMEOUT = NodeClient.TIMEOUT
    }

    @GetMapping("/list-available-artworks")
    suspend fun listAvailableArtworks(
        @RequestParam("galleryParty") galleryParty: ArtworkParty
    ) : ResponseEntity<List<ArtworkId>> {
        val artworkIds = artNetworkGalleryClient.listAvailableArtworks(galleryParty)
        return ResponseEntity.status(HttpStatus.OK).body(artworkIds)
    }
}