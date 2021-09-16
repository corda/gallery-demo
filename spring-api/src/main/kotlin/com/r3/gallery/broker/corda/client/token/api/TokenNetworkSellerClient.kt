package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.TransactionHash
import com.r3.gallery.api.TransactionSignature

interface TokenNetworkSellerClient {

    fun claimTokens(
        sellerParty: TokenParty,
        currency: String,
        encumberedTokens: TransactionHash,
        notarySignature: TransactionSignature
    ): TransactionHash

    fun releaseTokens(seller: TokenParty, currency: String, encumberedTokens: TransactionHash): TransactionHash

}