package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST endpoints for Gallery parties on Auction Network
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@RequestMapping("/gallery")
class ArtNetworkGalleryController(private val galleryClient: ArtNetworkGalleryClient) {
    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkGalleryController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    @PutMapping("/issue-artwork")
    fun issueArtwork(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: String
    ) : ResponseEntity<ArtworkOwnership> {
        logger.info("Request by $galleryParty to issue artwork of id $artworkId")
        val artworkOwnership = galleryClient.issueArtwork(galleryParty, artworkId.toUUID())
        return asResponse(artworkOwnership)
    }

    @GetMapping("/list-available-artworks")
    fun listAvailableArtworks(
        @RequestParam("galleryParty") galleryParty: ArtworkParty
    ) : ResponseEntity<List<ArtworkId>> {
        logger.info("Request of artwork listing of $galleryParty")
        val artworkIds = galleryClient.listAvailableArtworks(galleryParty)
        return asResponse(artworkIds)
    }

    @PutMapping("/create-artwork-transfer-tx")
    fun createArtworkTransferTx(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("bidderParty") bidderParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: String
    ) : ResponseEntity<UnsignedArtworkTransferTx> {
        // TODO: Is the DTO for ownership to be provided in full? or shall artworkId be used as is here?
        logger.info("Request to create artwork transfer transaction seller: $galleryParty, bidder: $bidderParty, art: $artworkId")
        val artworkOwnership = galleryClient.getOwnership(galleryParty, artworkId.toUUID())
        val artworkTx = galleryClient.createArtworkTransferTx(galleryParty, bidderParty, artworkOwnership)
        return asResponse(artworkTx)
    }

    @PostMapping("/finalise-artwork-transfer", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun finaliseArtworkTransfer(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestBody unsignedArtworkTransferTx: UnsignedArtworkTransferTx
    ) : ResponseEntity<ProofOfTransferOfOwnership> {
        logger.info("Request to finalise artwork transfer by $galleryParty for tx: $unsignedArtworkTransferTx")
        val proofOfTransfer = galleryClient.finaliseArtworkTransferTx(galleryParty, unsignedArtworkTransferTx)
        return asResponse(proofOfTransfer)
    }
}