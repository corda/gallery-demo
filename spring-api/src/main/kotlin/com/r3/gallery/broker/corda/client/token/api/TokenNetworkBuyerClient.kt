package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.EncumberedTokens
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.UnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClientImpl
import com.r3.gallery.workflows.token.IssueTokensFlow

interface TokenNetworkBuyerClient {

    fun issueTokens(buyer: TokenParty, amount: Long, currency: String)

    fun transferEncumberedTokens(buyer: TokenParty,
                                         seller: TokenParty,
                                         amount: Int,
                                         lockedOn: UnsignedArtworkTransferTx): EncumberedTokens
}