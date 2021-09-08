package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.broker.services.api.Receipt.BidReceipt

interface BidReceiptRepository {
    fun store(bidReceipt: BidReceipt)
    fun retrieve(bidderName: String, artworkId: ArtworkId): BidReceipt
    fun remove(bidderName: String, artworkId: ArtworkId)
    fun getBidsFor(artworkId: ArtworkId): List<BidReceipt>
}