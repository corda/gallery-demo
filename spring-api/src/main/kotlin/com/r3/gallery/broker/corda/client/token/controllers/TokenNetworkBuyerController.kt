package com.r3.gallery.broker.corda.client.token.controllers

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST endpoints for Buyers on a consideration (GBP or CBDC) network.
 */
@CrossOrigin
@RestController
@RequestMapping("/buyer")
class TokenNetworkBuyerController(private val buyerClient: TokenNetworkBuyerClient) {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkBuyerController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    /**
     * REST endpoint to Issue tokens on a consideration network
     *
     * TODO - NOTE: not currently supported in UI. This endpoint is for future use.
     *
     * @param buyerParty to issue the tokens
     * @param amount of tokens
     * @param currency description of the token type
     */
    @PutMapping("/issue-tokens")
    fun issueTokens(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("amount") amount: Int,
        @RequestParam("currency") currency: String
    ): ResponseEntity<Unit> {
        logger.info("Request by $buyerParty to issue $amount $currency to self")
        buyerClient.issueTokens(buyerParty, amount.toLong(), currency)
        return asResponse(Unit)
    }

    /**
     * TODO: This is an intermediate request endpoint used to test and will move to BidService placeBid
     * REST endpoint to transfer encumbered tokens after receiving a draft artwork transfer response
     *
     * @param buyerParty to create the encumbered transaction
     * @param sellerParty who will receive the encumbered tokens
     * @param amount of tokens
     * @param currency description of the token type
     * @param validatedUnsignedArtworkTransferTx to use for the lock on the encumbrance
     */
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

    /**
     * TODO: Note, this manual release is not currently implemented in the UI. The scenario is if the seller accepts no winner.
     * REST endpoint for releasing tokens at buyer request after an auction expiry where the seller did not respond
     * with a proof-of-action.
     *
     * @param buyerParty requesting release
     * @param currency of tokens
     * @param encumberedTokensTxHash hash of the encumbered tokens transaction.
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