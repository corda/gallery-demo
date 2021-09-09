package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.ArtworkOwnership
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST endpoints for Gallery parties on Auction Network
 */
@CrossOrigin
@RestController
@RequestMapping("/bidder")
class ArtNetworkBidderController(private val bidderClient: ArtNetworkBidderClient) {
    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkBidderController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    @PutMapping("/issue-artwork")
    fun issueArtwork(
        @RequestParam("bidderParty") bidderParty: ArtworkParty,
        @RequestParam("amount") amount: Long,
        @RequestParam("currency") currency: String
    ) : ResponseEntity<Unit> {
        logger.info("Request by $bidderParty to issue tokens for $amount $currency")
        bidderClient.issueTokens(bidderParty, amount, currency)
        return asResponse(Unit)
    }
}