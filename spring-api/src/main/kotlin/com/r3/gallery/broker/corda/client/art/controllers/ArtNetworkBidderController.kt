package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.BidProposal
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.BidService
import com.r3.gallery.utils.AuctionCurrency
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlin.math.pow

/**
 * REST endpoints for Bidder parties on Auction Network
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@RequestMapping("/bidder")
class ArtNetworkBidderController(private val bidderClient: ArtNetworkBidderClient) {

    @Autowired
    private lateinit var bidService: BidService

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkBidderController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    /**
     * TODO migrate intermediate requests to BidService
     * Places a bid on a piece of artwork run by a bidder on the auction network.
     *
     * @param bidProposal : bidderParty, artworkId, amount, currency
     * @return [Unit] OK 200 response if successful otherwise will throw error.
     */
    @PostMapping("/bid")
    fun bid(
        @RequestBody bidProposal: BidProposal
    ) : ResponseEntity<Unit> {
        logger.info("Request by ${bidProposal.bidderParty} to bid on ${bidProposal.artworkId} in amount of ${bidProposal.amount} ${bidProposal.currency}")

        // convert representation
        val bidAmount = bidProposal.amount.toDouble()
        val bidAmountLong = (bidAmount*10.0.pow(AuctionCurrency.getInstance(bidProposal.currency).fractionDigits)).toLong()
        bidService.placeBid(bidProposal.bidderParty, bidProposal.artworkId, bidAmountLong, bidProposal.currency)

        return asResponse(Unit)
    }

    /**
     *  TODO: TESTING only - Move to BidService placeBid (request-draft-transfer / transfer-encumbered tokens)
     */
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