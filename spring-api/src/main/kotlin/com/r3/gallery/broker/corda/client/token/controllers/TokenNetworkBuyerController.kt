package com.r3.gallery.broker.corda.client.token.controllers

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.art.controllers.ArtNetworkBidderController
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/buyer")
class TokenNetworkBuyerController(private val buyerClient: TokenNetworkBuyerClient) {

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkBidderController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    @PutMapping("/issue-tokens")
    fun issueTokens(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("amount") amount: Long,
        @RequestParam("currency") currency: String
    ): ResponseEntity<Unit> {
        logger.info("Request by $buyerParty to issue $amount $currency to self")
        buyerClient.issueTokens(buyerParty, amount, currency)
        return asResponse(Unit)
    }

    // TODO: Move to BidService placeBid (request-draft-transfer / transfer-encumbered tokens)
    @PostMapping("/transfer-encumbered-tokens", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun transferEncumberedTokens2(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestParam("amount") amount: Int,
        @RequestBody validatedUnsignedArtworkTransferTx: ValidatedUnsignedArtworkTransferTx
    ): ResponseEntity<TransactionHash> {
        logger.info("Request by $buyerParty to issue tokens for $amount")
        val signedTokenTransferTxId =
            buyerClient.transferEncumberedTokens(buyerParty, sellerParty, amount, validatedUnsignedArtworkTransferTx)
        return asResponse(signedTokenTransferTxId)
    }
}