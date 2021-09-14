package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.*

interface TokenNetworkSellerClient {

    fun claimTokens(sellerParty: TokenParty,
                    encumberedTokens: TransactionHash,
                    notarySignature: TransactionSignature): TransactionHash

    fun releaseTokens(
        sellerParty: TokenParty,
        buyer: TokenParty,
        encumberedTokens: EncumberedTokens
    ): CordaReference

}