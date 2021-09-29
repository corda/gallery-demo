package com.r3.gallery.broker.corda.client.token.controllers

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.asResponse
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.broker.services.BidService
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

/**
 * REST endpoints for Buyers on a consideration (GBP or CBDC) network.
 *
 * These endpoints are for testing only and are not called by bundled UI as the are processes executed via the
 * [BidService]. They can be called directly to control certain stages of the atomic swap.
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
     * @param buyerParty to issue the tokens
     * @param amount of tokens
     * @param currency description of the token type
     * @return [SignedTransaction] as a future
     */
    @PutMapping("/issue-tokens")
    fun issueTokens(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("amount") amount: Int,
        @RequestParam("currency") currency: String
    ): CompletableFuture<ResponseEntity<SignedTransaction>> {
        logger.info("Request by $buyerParty to issue $amount $currency to self")
        return CompletableFuture.supplyAsync {
            buyerClient.issueTokens(buyerParty, amount.toLong(), currency).toCompletableFuture().join()
        }.thenApply {
            asResponse(it)
        }
    }

    /**
     * REST endpoint to transfer encumbered tokens after receiving a draft artwork transfer response
     *
     * @param buyerParty to create the encumbered transaction
     * @param sellerParty who will receive the encumbered tokens
     * @param amount of tokens
     * @param currency description of the token type
     * @param validatedUnsignedArtworkTransferTx to use for the lock on the encumbrance
     * @return [TransactionHash] as a future
     */
    @PostMapping("/transfer-encumbered-tokens", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun transferEncumberedTokens(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestParam("amount") amount: Long,
        @RequestParam("currency") currency: String,
        @RequestBody validatedUnsignedArtworkTransferTx: ValidatedUnsignedArtworkTransferTx
    ): CompletableFuture<ResponseEntity<TransactionHash>> {
        logger.info("Request by $buyerParty to issue tokens for $amount")
        return CompletableFuture.supplyAsync {
            buyerClient.transferEncumberedTokens(buyerParty, sellerParty, amount, currency, validatedUnsignedArtworkTransferTx)
        }.thenApply {
            asResponse(it)
        }
    }

    /**
     * REST endpoint for releasing tokens at buyer request after an auction expiry where the seller did not respond
     * with a proof-of-action.
     *
     * @param buyerParty requesting release
     * @param currency of tokens
     * @param encumberedTokensTxHash hash of the encumbered tokens transaction.
     * @return [TransactionHash] as a future
     */
    @PostMapping("/release-encumbered-tokens")
    fun transferEncumberedTokens(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("currency") currency: String,
        @RequestParam("encumberedTokensTxHash") encumberedTokensTxHash: String,
    ): CompletableFuture<ResponseEntity<TransactionHash>> {
        logger.info("Request by $buyerParty to release unspent tokens from encumbered offer $encumberedTokensTxHash")
        return CompletableFuture.supplyAsync {
            buyerClient.releaseTokens(buyerParty, currency, encumberedTokensTxHash)
        }.thenApply {
            asResponse(it)
        }
    }
}