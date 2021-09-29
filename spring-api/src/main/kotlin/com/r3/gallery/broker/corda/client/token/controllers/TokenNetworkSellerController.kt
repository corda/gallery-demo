package com.r3.gallery.broker.corda.client.token.controllers

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TokenReleaseData
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.broker.services.BidService
import com.r3.gallery.broker.corda.client.asResponse
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

/**
 * REST endpoints for Sellers on a consideration (GBP or CBDC) network.
 *
 * These endpoints are for testing only and are not called by bundled UI as the are processes executed via the
 * [BidService]. They can be called directly to control certain stages of the atomic swap.
 */
@CrossOrigin
@RestController
@RequestMapping("/seller")
class TokenNetworkSellerController(private val sellerClient: TokenNetworkSellerClient) {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkSellerController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    /**
     * REST endpoint for seller to claim tokens after finalising a draft transfer of artwork which satisfies the encumbrance
     * lock of a bid.
     *
     * @param sellerParty making the claim
     * @param currency of the tokens
     * @param tokenReleaseData object containing encumberedTokens tx hash and notary signature proof-of-action.
     * @return [TransactionHash] as a future
     */
    @PostMapping("/claim-tokens", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun claimTokens(
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestParam("currency") currency: String,
        @RequestBody tokenReleaseData: TokenReleaseData
    ): CompletableFuture<ResponseEntity<TransactionHash>> {
        logger.info("Request by $sellerParty to claim tokens from encumbered tx ${tokenReleaseData.encumberedTokens} with signature ${tokenReleaseData.notarySignature}")
        return CompletableFuture.supplyAsync {
            sellerClient.claimTokens(sellerParty, currency, tokenReleaseData.encumberedTokens, tokenReleaseData.notarySignature)
        }.thenApply {
            asResponse(it)
        }
    }

    /**
     * REST endpoint to release tokens which are pending POA/encumbered in the case that another bid was accepted, or
     * that the seller no longer wishes to continue the auction. In this case these tokens will be reverted/released
     * to original bidders.
     *
     * @param sellerParty initiating the release and listed as 'receiver' of the tokens on the lock
     * @param currency of the tokens
     * @param encumberedTokensTxHash
     * @return [TransactionHash] as a future
     */
    @PostMapping("/release-encumbered-tokens")
    fun releaseTokens(
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestParam("currency") currency: String,
        @RequestParam("encumberedTokensTxHash") encumberedTokensTxHash: String,
    ): CompletableFuture<ResponseEntity<TransactionHash>> {
        logger.info("Request by $sellerParty to release unspent tokens from encumbered offer $encumberedTokensTxHash")
        return CompletableFuture.supplyAsync {
            sellerClient.releaseTokens(sellerParty, currency, encumberedTokensTxHash)
        }.thenApply {
            asResponse(it)
        }
    }
}