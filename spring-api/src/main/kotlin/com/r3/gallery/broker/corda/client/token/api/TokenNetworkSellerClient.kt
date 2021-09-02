package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.CordaReference
import com.r3.gallery.api.EncumberedTokens
import com.r3.gallery.api.ProofOfTransferOfOwnership
import com.r3.gallery.api.TokenParty

interface TokenNetworkSellerClient {

    suspend fun claimTokens(
        sellerParty: TokenParty,
        encumberedTokens: EncumberedTokens,
        proofOfTransfer: ProofOfTransferOfOwnership): CordaReference

    suspend fun releaseTokens(
        sellerParty: TokenParty,
        buyer: TokenParty,
        encumberedTokens: EncumberedTokens): CordaReference

}