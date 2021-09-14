package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.*

interface TokenNetworkBuyerClient {

    fun issueTokens(buyer: TokenParty, amount: Long, currency: String)

    fun transferEncumberedTokens(buyer: TokenParty,
                                 seller: TokenParty,
                                 amount: Int,
                                 lockedOn: ValidatedUnsignedArtworkTransferTx): TransactionHash
}