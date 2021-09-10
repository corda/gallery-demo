package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.EncumberedTokens
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.UnsignedArtworkTransferTx
import com.r3.gallery.api.UnsignedArtworkTransferTxAndLock
import com.r3.gallery.states.LockState

interface TokenNetworkBuyerClient {

    fun issueTokens(buyer: TokenParty, amount: Long, currency: String)

    fun transferEncumberedTokens(buyer: TokenParty,
                                         seller: TokenParty,
                                         amount: Int,
                                         lockedOn: UnsignedArtworkTransferTx): EncumberedTokens

    fun transferEncumberedTokens2(buyer: TokenParty,
                                 seller: TokenParty,
                                 amount: Int,
                                 lockedOn: UnsignedArtworkTransferTxAndLock): EncumberedTokens
}