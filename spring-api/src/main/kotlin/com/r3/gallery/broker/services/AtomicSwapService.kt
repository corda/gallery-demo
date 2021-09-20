package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.broker.services.api.Receipt.*

const val GALLERY = "O=Alice, L=London, C=GB"

interface AtomicSwapService {
    fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String): BidReceipt
    fun awardArtwork(bid: BidReceipt, currency: String): SaleReceipt
    fun cancelBid(bid: BidReceipt, currency: String): CancellationReceipt
}
