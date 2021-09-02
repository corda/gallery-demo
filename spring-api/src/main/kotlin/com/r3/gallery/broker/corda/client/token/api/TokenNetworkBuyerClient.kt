package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.EncumberedTokens
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.UnsignedArtworkTransferTx

interface TokenNetworkBuyerClient {

    suspend fun transferEncumberedTokens(buyer: TokenParty,
                                         seller: TokenParty,
                                         amount: Int,
                                         lockedOn: UnsignedArtworkTransferTx): EncumberedTokens
}