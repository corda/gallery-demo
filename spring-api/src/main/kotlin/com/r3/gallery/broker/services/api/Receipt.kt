package com.r3.gallery.broker.services.api

import com.r3.gallery.api.*

sealed class Receipt {

    abstract val bidderName: String
    abstract val artworkId: ArtworkId

    data class BidReceipt(
        override val bidderName: String,
        override val artworkId: ArtworkId,
        val unsignedArtworkTransferTx: UnsignedArtworkTransferTx,
        val encumberedTokens: TransactionHash
    ) : Receipt()

    data class SaleReceipt(
        override val bidderName: String,
        override val artworkId: ArtworkId,
        val transferTxId: TransactionHash,
        val tokenTxId: TransactionHash
    ) : Receipt()

    data class CancellationReceipt(
        override val bidderName: String,
        override val artworkId: ArtworkId,
        val transferTxId: TransactionHash
    ) : Receipt()
}