package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.*
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
class ArtNetworkGalleryController(private val galleryClient: ArtNetworkGalleryClient) {
    companion object {
        // TODO: Add logs to each call
        private val logger = LoggerFactory.getLogger(ArtNetworkGalleryController::class.java)
        const val TIMEOUT = NodeClient.TIMEOUT
    }

    @PutMapping("/issue-artwork")
    suspend fun issueArtwork(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: ArtworkId
    ) : ResponseEntity<ArtworkOwnership> {
        val artworkOwnership = galleryClient.issueArtwork(galleryParty, artworkId)
        return ResponseEntity.status(HttpStatus.OK).body(artworkOwnership)
    }

    @GetMapping("/list-available-artworks")
    suspend fun listAvailableArtworks(
        @RequestParam("galleryParty") galleryParty: ArtworkParty
    ) : ResponseEntity<List<ArtworkId>> {
        val artworkIds = galleryClient.listAvailableArtworks(galleryParty)
        return ResponseEntity.status(HttpStatus.OK).body(artworkIds)
    }

    @PutMapping("/create-artwork-transfer-tx")
    suspend fun createArtworkTransferTx(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("bidderParty") bidderParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: ArtworkId,
    ) : ResponseEntity<UnsignedArtworkTransferTx> {
        // TODO: Is the DTO for ownership to be provided in full? or shall artworkId be used as is here?
        val artworkOwnership = galleryClient.getOwnership(galleryParty, artworkId)
        val artworkTx = galleryClient.createArtworkTransferTx(galleryParty, bidderParty, artworkOwnership)
        return ResponseEntity.status(HttpStatus.OK).body(artworkTx)
    }

    @PutMapping("/finalise-artwork-trans")
    suspend fun finaliseArtworkTransfer(
        galleryParty: ArtworkParty,
        unsignedArtworkTransferTx: UnsignedArtworkTransferTx
    ) : ResponseEntity<ProofOfTransferOfOwnership> {
        val proofOfTransfer = galleryClient.finaliseArtworkTransferTx(galleryParty, unsignedArtworkTransferTx)
        return ResponseEntity.status(HttpStatus.OK).body(proofOfTransfer)
    }
}