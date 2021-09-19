package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.BidProposal
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST endpoints for Bidder parties on Auction Network
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@RequestMapping("/bidder")
class ArtNetworkBidderController(private val bidderClient: ArtNetworkBidderClient) {

    @Autowired
    private lateinit var tokenBuyerClient: TokenNetworkBuyerClient

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

        val verifiedWireTx = bidderClient.requestDraftTransferOfOwnership(
            bidProposal.bidderParty,
            "O=Alice, L=London, C=GB",
            bidProposal.artworkId
        )
        tokenBuyerClient.transferEncumberedTokens(
            buyer = bidProposal.bidderParty,
            seller = "O=Alice, L=London, C=GB",
            amount = bidProposal.amount.toLong(),
            currency = "GBP",
            verifiedWireTx
        )
        return asResponse(Unit)
    }
}