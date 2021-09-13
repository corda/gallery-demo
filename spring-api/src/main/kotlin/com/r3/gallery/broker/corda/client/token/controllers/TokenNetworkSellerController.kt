package com.r3.gallery.broker.corda.client.token.controllers

import com.r3.gallery.api.CordaReference
import com.r3.gallery.api.StateRefAndSignature
import com.r3.gallery.api.TokenParty
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClientImpl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/seller")
class TokenNetworkSellerController(private val sellerClient: TokenNetworkSellerClientImpl) {

    @PostMapping("/claim-tokens", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun transferEncumberedTokens(
        @RequestParam("sellerParty") sellerParty: TokenParty,
        @RequestBody stateRefAndSignature: StateRefAndSignature
    ): ResponseEntity<CordaReference> {

        val encumberedTokens = stateRefAndSignature.encumberedTokens
        val proofOfTransfer = stateRefAndSignature.proofOfTransfer

        val cordaReference = sellerClient.claimTokens(sellerParty, encumberedTokens, proofOfTransfer)
        return asResponse(cordaReference)
    }
}