package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.BidProposal
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.BidService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST endpoints for Gallery parties on Auction Network
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "false")
@RequestMapping("/bidder")
class ArtNetworkBidderController(private val bidderClient: ArtNetworkBidderClient) {

//    @Autowired
//    private lateinit var bidService: BidService
    @Autowired
    private lateinit var tokenBuyerClient: TokenNetworkBuyerClient

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkBidderController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    @PostMapping("/bid")
    fun bid(
        @RequestBody bidProposal: BidProposal
    ) : ResponseEntity<Unit> {
        logger.info("Request by ${bidProposal.bidderParty} to bid on ${bidProposal.artworkId} in amount of ${bidProposal.amount} ${bidProposal.currency}")
        // TODO migrate to BidService
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

    // TODO: Move to BidService placeBid (request-draft-transfer / transfer-encumbered tokens)
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