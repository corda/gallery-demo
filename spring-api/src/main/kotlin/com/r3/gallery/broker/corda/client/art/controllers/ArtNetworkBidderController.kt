package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.BidProposal
import com.r3.gallery.broker.corda.client.deferredResult
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.BidService
import com.r3.gallery.utils.AuctionCurrency
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult
import kotlin.math.pow

/**
 * REST endpoints for Bidder parties on Auction Network
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@RequestMapping("/bidder")
class ArtNetworkBidderController() {

    @Autowired
    private lateinit var bidService: BidService

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkBidderController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    /**
     * Places a bid on a piece of artwork run by a bidder on the auction network.
     *
     * @param bidProposal : bidderParty, artworkId, amount, currency
     * @return [Unit] OK 200 response if successful otherwise will throw error.
     */
    @PostMapping("/bid")
    fun bid(
        @RequestBody bidProposal: BidProposal
    ) : DeferredResult<ResponseEntity<Unit>> {
        logger.info("Request by ${bidProposal.bidderParty} to bid on ${bidProposal.artworkId} in amount of ${bidProposal.amount} ${bidProposal.currency}")
        return deferredResult {
            // convert representation
            val bidAmount = bidProposal.amount.toDouble()
            val bidAmountLong = (bidAmount*10.0.pow(AuctionCurrency.getInstance(bidProposal.currency).fractionDigits)).toLong()
            bidService.placeBid(bidProposal.bidderParty, bidProposal.artworkId, bidAmountLong, bidProposal.currency)
        }
    }
}