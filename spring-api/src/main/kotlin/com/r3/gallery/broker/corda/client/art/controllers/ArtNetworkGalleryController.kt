package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.AcceptedBid
import com.r3.gallery.api.ArtworkOwnership
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.AvailableArtwork
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.deferredResult
import com.r3.gallery.broker.corda.client.toUUID
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.BidService
import net.corda.core.internal.toX500Name
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult

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
        @RequestParam("expiryDays", required = false) expiry: Int = 3,
        @RequestParam("description", required = false) description: String = "",
        @RequestParam("url", required = false) url: String = ""
    ): DeferredResult<ResponseEntity<ArtworkOwnership>> {
        logger.info("Request by $galleryParty to issue artwork of id $artworkId")
        return deferredResult {
            galleryClient.issueArtwork(
                    galleryParty,
                    artworkId.toUUID(),
                    expiry,
                    description,
                    url
            ).toCompletableFuture().thenApply {
                ArtworkOwnership(
                        it.linearId.id,
                        it.artworkId,
                        it.owner.nameOrNull()!!.toX500Name().toString()
                )
            }.get()
        }
    }

    /**
     * REST endpoint to list available artwork held by a particular gallery
     *
     * @param galleryParty to query for artwork
     */
    @GetMapping("/list-available-artworks")
    fun listAvailableArtworks(
        @RequestParam("galleryParty", required = false) galleryParty: ArtworkParty?
    ): DeferredResult<ResponseEntity<List<AvailableArtwork>>> {
        logger.info("Request of artwork listing of ${galleryParty?:"all galleries"}")
        return deferredResult {
            bidService.listAvailableArtworks(galleryParty ?: "O=Alice, L=London, C=GB")
        }
    }

    /**
     * REST endpoint to 'ACCEPT' an offer/bid for a piece of artwork
     *
     * @param acceptedBid (JSON)
     */
    @PostMapping("/accept-bid", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun acceptBid(
        @RequestBody acceptedBid: AcceptedBid
    ): DeferredResult<ResponseEntity<Unit>> {
        logger.info("Request by gallery to accept bid for $acceptedBid.artworkId from $acceptedBid.bidderParty")
        return deferredResult {
            bidService.awardArtwork(acceptedBid.bidderParty, acceptedBid.artworkId, acceptedBid.currency)
            Unit
        }
    }
}