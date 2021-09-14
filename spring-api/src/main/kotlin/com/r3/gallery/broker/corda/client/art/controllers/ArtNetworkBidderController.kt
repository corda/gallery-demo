package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
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

    @PutMapping("/request-draft-transfer")
    fun requestDraftTransfer(
        @RequestParam("bidderParty") bidderParty: ArtworkParty,
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: ArtworkId
    ): ResponseEntity<ValidatedUnsignedArtworkTransferTx> {
        logger.info("Request by $bidderParty to $galleryParty to a draft a transfer of ownership for $artworkId")
        val verifiedWireTx = bidderClient.requestDraftTransferOfOwnership(bidderParty, galleryParty, artworkId)
        return asResponse(verifiedWireTx)
    }
}