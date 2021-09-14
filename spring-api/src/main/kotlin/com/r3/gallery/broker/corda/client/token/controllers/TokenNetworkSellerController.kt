package com.r3.gallery.broker.corda.client.token.controllers

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TokenReleaseData
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClient
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/seller")
class TokenNetworkSellerController(private val sellerClient: TokenNetworkSellerClient) {

    @PostMapping("/claim-tokens", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun claimTokens(
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestBody tokenReleaseData: TokenReleaseData
    ): ResponseEntity<TransactionHash> {
        val transactionHash =
            sellerClient.claimTokens(sellerParty, tokenReleaseData.encumberedTokens, tokenReleaseData.notarySignature)
        return asResponse(transactionHash)
    }
}