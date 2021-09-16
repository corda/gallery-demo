package com.r3.gallery.broker.corda.client.token.controllers

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TokenReleaseData
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.broker.corda.client.art.controllers.ArtNetworkBidderController
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/seller")
class TokenNetworkSellerController(private val sellerClient: TokenNetworkSellerClient) {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkSellerController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    @PostMapping("/claim-tokens", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun claimTokens(
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestParam("currency") currency: String,
        @RequestBody tokenReleaseData: TokenReleaseData
    ): ResponseEntity<TransactionHash> {
        logger.info("Request by $sellerParty to release unspent tokens from encumbered offer $encumberedTokensTxHash")
        val transactionHash =
            sellerClient.claimTokens(sellerParty, currency, tokenReleaseData.encumberedTokens, tokenReleaseData.notarySignature)
        return asResponse(transactionHash)
    }

    /*
     * Releases the unspent encumbered tokens offer specified by encumberedTokensTxHash on the party that issued it
     */
    @PostMapping("/release-encumbered-tokens")
    fun transferEncumberedTokens(
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestParam("currency") currency: String,
        @RequestParam("encumberedTokensTxHash") encumberedTokensTxHash: String,
    ): ResponseEntity<TransactionHash> {
        logger.info("Request by $sellerParty to release unspent tokens from encumbered offer $encumberedTokensTxHash")
        val releasedTokensTxId = sellerClient.releaseTokens(sellerParty, currency, encumberedTokensTxHash)
        return asResponse(releasedTokensTxId)
    }
}