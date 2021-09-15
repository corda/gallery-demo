package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.*

interface TokenNetworkSellerClient {

    fun claimTokens(sellerParty: TokenParty,
                    currency: String,
                    encumberedTokens: TransactionHash,
                    notarySignature: TransactionSignature): TransactionHash

    fun releaseTokens(
        sellerParty: TokenParty,
        buyer: TokenParty,
        currency: String,
        encumberedTokens: EncumberedTokens
    ): CordaReference

}