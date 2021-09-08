package com.r3.gallery.broker.corda.client.token.controllers

import com.r3.gallery.api.EncumberedTokens
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.UnsignedArtworkTransferTx
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

    @PostMapping("/transfer-encumbered-tokens", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun transferEncumberedTokens(
        @RequestParam("buyerParty") buyerParty: TokenParty,
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestParam("amount") amount: Int,
        @RequestBody tx: ByteArray) : ResponseEntity<EncumberedTokens> {
        logger.info("Request by $buyerParty to issue tokens for $amount")
        val encumberedTokens = buyerClient.transferEncumberedTokens(buyerParty, sellerParty, amount,UnsignedArtworkTransferTx(tx))
        return asResponse(encumberedTokens)
    }
}