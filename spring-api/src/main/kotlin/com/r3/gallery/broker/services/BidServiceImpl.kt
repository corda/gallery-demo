package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.AvailableArtwork
import com.r3.gallery.broker.services.api.Receipt
import com.r3.gallery.utils.AuctionCurrency
import net.corda.core.contracts.Amount
import net.corda.core.internal.hash
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
class BidServiceImpl(
    @Autowired val swapService: AtomicSwapService,
    @Autowired val bidRepository: ReceiptRepository<Receipt.BidReceipt>
) : BidService {

    companion object {
        private val logger = LoggerFactory.getLogger(BidServiceImpl::class.java)
    }

    override fun placeBid(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String) {
        logger.info("Processing bid $bidderName, $artworkId, $bidAmount, $currency in BidService")

        val bidReceipt = swapService.bidForArtwork(bidderName, artworkId, bidAmount, currency)

        bidRepository.store(bidReceipt)
    }

    override fun awardArtwork(bidderName: String, artworkId: ArtworkId, encumberedCurrency: String): List<Receipt> {
        logger.info("Processing award artwork $bidderName, $artworkId, $encumberedCurrency in BidService")

        val bidReceipt = bidRepository.retrieve(bidderName, artworkId)
        val saleReceipt = swapService.awardArtwork(bidReceipt, encumberedCurrency)

        bidRepository.remove(bidderName, artworkId)

        // cancel remaining bids
        val cancelReceipts = bidRepository.getBidsFor(artworkId).map { failedBid ->
            swapService.cancelBid(failedBid, encumberedCurrency)
        }

        return listOf(saleReceipt) + cancelReceipts
    }

    /**
     * TODO populate bids on spring restart by using ledger.
     * Lists available artwork held by a particular gallery
     *
     * @param galleryParty to query for artwork
     * @return [List][AvailableArtwork]
     */
    override fun listAvailableArtworks(galleryParty: ArtworkParty): List<AvailableArtwork> {
        logger.info("Listing available artworks via $galleryParty")
        val artworks = swapService.getAllArtworks()
        return artworks.map { artwork ->
            val bids = bidRepository.getBidsFor(artwork.artworkId).map { bid ->
                val bidder = swapService.getPartyFromNameAndCurrency(bid.bidderName, bid.currency)
                AvailableArtwork.BidRecord(
                        cordaReference =  bid.encumberedTokens,
                        bidderPublicKey = bidder.owningKey.hash.toString(),
                        bidderDisplayName = bid.bidderName,
                        amountAndCurrency = Amount(bid.amount, AuctionCurrency.getInstance(bid.currency)),
                        notary = "${bid.currency} Notary",
                        accepted = false // TODO check if there is a SALE receipt
                )
            }
            AvailableArtwork(
                artworkId = artwork.artworkId,
                description = artwork.description,
                url = artwork.url,
                listed = true,
                expiryDate = Date.from(artwork.expiry),
                bids = bids
            )
        }
    }
}