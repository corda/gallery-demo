package com.r3.gallery.broker.services

import com.r3.gallery.broker.corda.client.api.ArtworkId
import com.r3.gallery.broker.services.api.Receipt
import org.springframework.beans.factory.annotation.Autowired

class BidService(
    @Autowired val swapService: AtomicSwapService,
    @Autowired val bidRepository: BidReceiptRepository
) {

    suspend fun placeBid(bidderName: String, artworkId: ArtworkId, bidAmount: Int) {
        val bidReceipt = swapService.bidForArtwork(bidderName, artworkId, bidAmount)

        bidRepository.store(bidReceipt)
    }

    suspend fun awardArtwork(bidderName: String, artworkId: ArtworkId): List<Receipt> {
        val bidReceipt = bidRepository.retrieve(bidderName, artworkId)
        val saleReceipt = swapService.awardArtwork(bidReceipt)

        bidRepository.remove(bidderName, artworkId)

        // cancel remaining bids
        val cancelReceipts = bidRepository.getBidsFor(artworkId).map { failedBid ->
            swapService.cancelBid(failedBid)
        }

        return listOf(saleReceipt) + cancelReceipts
    }
}