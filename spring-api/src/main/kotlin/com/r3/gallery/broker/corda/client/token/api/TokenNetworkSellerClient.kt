package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.*

interface TokenNetworkSellerClient {

    fun claimTokens(
        sellerParty: TokenParty,
        encumberedTokens: EncumberedTokens,
        proofOfTransfer: ProofOfTransferOfOwnership
    ): CordaReference

    fun claimTokens2(sellerParty: TokenParty, tokenReleaseData: TokenReleaseData): CordaReference

    fun releaseTokens(
        sellerParty: TokenParty,
        buyer: TokenParty,
        encumberedTokens: EncumberedTokens
    ): CordaReference

}