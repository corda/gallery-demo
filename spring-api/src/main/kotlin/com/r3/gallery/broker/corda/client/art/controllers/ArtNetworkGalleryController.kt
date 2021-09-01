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
        private val logger = LoggerFactory.getLogger(ArtNetworkGalleryController::class.java)
        const val TIMEOUT = NodeClient.TIMEOUT
    }

    @PutMapping("/issue-artwork")
    suspend fun issueArtwork(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: ArtworkId
    ) : ResponseEntity<ArtworkOwnership> {
        logger.info("Request by $galleryParty to issue artwork of id $artworkId")
        val artworkOwnership = galleryClient.issueArtwork(galleryParty, artworkId)
        return ResponseEntity.status(HttpStatus.OK).body(artworkOwnership)
    }

    @GetMapping("/list-available-artworks")
    suspend fun listAvailableArtworks(
        @RequestParam("galleryParty") galleryParty: ArtworkParty
    ) : ResponseEntity<List<ArtworkId>> {
        logger.info("Request of artwork listing of $galleryParty")
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
        logger.info("Request to create artwork transfer transaction seller: $galleryParty, bidder: $bidderParty, art: $artworkId")
        val artworkOwnership = galleryClient.getOwnership(galleryParty, artworkId)
        val artworkTx = galleryClient.createArtworkTransferTx(galleryParty, bidderParty, artworkOwnership)
        return ResponseEntity.status(HttpStatus.OK).body(artworkTx)
    }

    @PutMapping("/finalise-artwork-trans")
    suspend fun finaliseArtworkTransfer(
        galleryParty: ArtworkParty,
        unsignedArtworkTransferTx: UnsignedArtworkTransferTx
    ) : ResponseEntity<ProofOfTransferOfOwnership> {
        logger.info("Request to finalise artwork transfer by $galleryParty for tx: $unsignedArtworkTransferTx")
        val proofOfTransfer = galleryClient.finaliseArtworkTransferTx(galleryParty, unsignedArtworkTransferTx)
        return ResponseEntity.status(HttpStatus.OK).body(proofOfTransfer)
    }
}