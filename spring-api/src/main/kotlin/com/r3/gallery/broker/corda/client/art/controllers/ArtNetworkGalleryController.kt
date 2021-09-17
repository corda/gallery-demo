package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant

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
        @RequestParam("artworkId") artworkId: String,
        @RequestParam("expiryDays") expiry: Int,
        @RequestParam("description", required = false) description: String = "",
        @RequestParam("url", required = false) url: String = ""
    ): ResponseEntity<ArtworkOwnership> {
        logger.info("Request by $galleryParty to issue artwork of id $artworkId")
        val expInstant = Instant.now().plus(Duration.ofDays(3))
        val artworkOwnership = galleryClient.issueArtwork(
            galleryParty,
            artworkId.toUUID(),
            expInstant,
            description,
            url
        )
        return asResponse(artworkOwnership)
    }

    @GetMapping("/list-available-artworks")
    fun listAvailableArtworks(
        @RequestParam("galleryParty", required = false) galleryParty: ArtworkParty?
    ): ResponseEntity<List<AvailableArtwork>> {
        logger.info("Request of artwork listing of $galleryParty")
        val artworks = galleryClient.listAvailableArtworks(galleryParty ?: "O=Alice, L=London, C=GB")
        return asResponse(artworks)
    }

    // TODO: Send to BidService awardArtwork
    @PostMapping("/accept-bid", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun acceptBid(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("cordaReference") cordaReference: CordaReference
    ) : ResponseEntity<Unit> {
        logger.info("Request by $galleryParty to accept bid from $cordaReference")
        return asResponse(Unit)
    }

    // TODO: Move to bidService awardArtwork
    @PostMapping("/finalise-artwork-transfer", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun finaliseArtworkTransfer(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestBody unsignedArtworkTransferTx: UnsignedArtworkTransferTx
    ): ResponseEntity<ProofOfTransferOfOwnership> {
        logger.info("Request to finalise artwork transfer by $galleryParty for tx: $unsignedArtworkTransferTx")
        val proofOfTransfer = galleryClient.finaliseArtworkTransferTx(galleryParty, unsignedArtworkTransferTx)
        return asResponse(proofOfTransfer)
    }
}