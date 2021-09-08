package com.r3.gallery.broker.services.api

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.CordaReference
import com.r3.gallery.api.EncumberedTokens
import com.r3.gallery.api.UnsignedArtworkTransferTx

sealed class Receipt {

    abstract val bidderName: String
    abstract val artworkId: ArtworkId

    data class BidReceipt(
        override val bidderName: String,
        override val artworkId: ArtworkId,
        val unsignedArtworkTransferTx: UnsignedArtworkTransferTx,
        val encumberedTokens: EncumberedTokens
    ) : Receipt()

    data class SaleReceipt(
        override val bidderName: String,
        override val artworkId: ArtworkId,
        val transferTxId: CordaReference,
        val tokenTxId: CordaReference
    ) : Receipt()

    data class CancellationReceipt(
        override val bidderName: String,
        override val artworkId: ArtworkId,
        val transferTxId: CordaReference
    ) : Receipt()
}