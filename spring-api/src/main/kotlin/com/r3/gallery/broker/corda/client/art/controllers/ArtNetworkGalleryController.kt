package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.BidService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    private lateinit var bidService: BidService

    /**
     * REST endpoint to Issue an artwork
     *
     * TODO - NOTE: not currently supported in UI. This endpoint is for future use.
     *
     * @param galleryParty to issue the artwork
     * @param artworkId unique identifier of the artwork (valid UUID format)
     * @param expiry OPTIONAL number of days to auction the artwork for
     * @param description OPTIONAL
     * @param url OPTIONAL uri to an image or asset
     */
    @PutMapping("/issue-artwork")
    fun issueArtwork(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: String,
        @RequestParam("expiryDays", required = false) expiry: Int?,
        @RequestParam("description", required = false) description: String = "",
        @RequestParam("url", required = false) url: String = ""
    ): ResponseEntity<ArtworkOwnership> {
        logger.info("Request by $galleryParty to issue artwork of id $artworkId")
        val expInstant = Instant.now().plus(Duration.ofDays(expiry?.toLong() ?: 3))
        val artworkOwnership = galleryClient.issueArtwork(
            galleryParty,
            artworkId.toUUID(),
            expInstant,
            description,
            url
        )
        return asResponse(artworkOwnership)
    }

    /**
     * REST endpoint to list available artwork held by a particular gallery
     *
     * @param galleryParty to query for artwork
     */
    @GetMapping("/list-available-artworks")
    fun listAvailableArtworks(
        @RequestParam("galleryParty", required = false) galleryParty: ArtworkParty?
    ): ResponseEntity<List<AvailableArtwork>> {
        logger.info("Request of artwork listing of ${galleryParty?:"all galleries"}")
        val artworks = bidService.listAvailableArtworks(galleryParty ?: "O=Alice, L=London, C=GB")
        return asResponse(artworks)
    }

    /**
     * TODO: Forward request to bidService
     * REST endpoint to 'ACCEPT' an offer/bid for a piece of artwork
     *
     * @param acceptedBid (JSON)
     */
    @PostMapping("/accept-bid", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun acceptBid(
        @RequestBody acceptedBid: AcceptedBid
    ) : ResponseEntity<Unit> {
        logger.info("Request by gallery to accept bid for $acceptedBid.artworkId from $acceptedBid.bidderParty")
        bidService.awardArtwork(acceptedBid.bidderParty, acceptedBid.artworkId, acceptedBid.currency)
        return asResponse(Unit)
    }

    /**
     * TODO: This is an intermediate request test endpoint and will move to bidService awardArtwork
     * REST endpoint to finalize (deserialize, sign, and generate proof of ownership) an [UnsignedArtworkTransferTx]
     *
     * @param galleryParty who owns the artwork
     * @param unsignedArtworkTransferTx representing the transaction to transfer the asset.
     */
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