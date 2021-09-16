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
        private val logger = LoggerFactory.getLogger(TokenNetworkBuyerController::class.java)
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

    @PostMapping("/transfer-encumbered-tokens", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun transferEncumberedTokens(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestParam("amount") amount: Long,
        @RequestParam("currency") currency: String,
        @RequestBody validatedUnsignedArtworkTransferTx: ValidatedUnsignedArtworkTransferTx
    ): ResponseEntity<TransactionHash> {
        logger.info("Request by $buyerParty to issue tokens for $amount")
        val signedTokenTransferTxId =
            buyerClient.transferEncumberedTokens(buyerParty, sellerParty, amount, currency, validatedUnsignedArtworkTransferTx)
        return asResponse(signedTokenTransferTxId)
    }

    /*
     * Releases the unspent encumbered tokens offer specified by encumberedTokensTxHash on the party that issued it
     */
    @PostMapping("/release-encumbered-tokens")
    fun transferEncumberedTokens(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("currency") currency: String,
        @RequestParam("encumberedTokensTxHash") encumberedTokensTxHash: String,
    ): ResponseEntity<TransactionHash> {
        logger.info("Request by $buyerParty to release unspent tokens from encumbered offer $encumberedTokensTxHash")
        val releasedTokensTxId = buyerClient.releaseTokens(buyerParty, currency, encumberedTokensTxHash)
        return asResponse(releasedTokensTxId)
    }
}