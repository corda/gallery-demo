package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.broker.services.api.Receipt
import org.springframework.beans.factory.annotation.Autowired

class BidService(
    @Autowired val swapService: AtomicSwapService,
    @Autowired val bidRepository: ReceiptRepository<Receipt.BidReceipt>
) {

    fun placeBid(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String) {
        val bidReceipt = swapService.bidForArtwork(bidderName, artworkId, bidAmount, currency)

        bidRepository.store(bidReceipt)
    }

    fun awardArtwork(bidderName: String, artworkId: ArtworkId, encumberedCurrency: String): List<Receipt> {
        val bidReceipt = bidRepository.retrieve(bidderName, artworkId)
        val saleReceipt = swapService.awardArtwork(bidReceipt, encumberedCurrency)

        bidRepository.remove(bidderName, artworkId)

        // cancel remaining bids
        val cancelReceipts = bidRepository.getBidsFor(artworkId).map { failedBid ->
            swapService.cancelBid(failedBid, encumberedCurrency)
        }

        return listOf(saleReceipt) + cancelReceipts
    }
}