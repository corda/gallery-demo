package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.broker.services.api.Receipt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BidServiceImpl(
    @Autowired val swapService: AtomicSwapService,
    @Autowired val bidRepository: ReceiptRepository<Receipt.BidReceipt>
) : BidService {

    override fun placeBid(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String) {

        val bidReceipt = swapService.bidForArtwork(bidderName, artworkId, bidAmount, currency)

        bidRepository.store(bidReceipt)
    }

    override fun awardArtwork(bidderName: String, artworkId: ArtworkId, encumberedCurrency: String): List<Receipt> {
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