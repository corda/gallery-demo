package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.AvailableArtwork
import com.r3.gallery.api.NetworkBalancesResponse
import com.r3.gallery.broker.services.api.Receipt
import com.r3.gallery.utils.AuctionCurrency
import net.corda.core.contracts.Amount
import net.corda.core.internal.hash
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

@Component
class BidServiceImpl(
    @Autowired val swapService: AtomicSwapService,
    @Autowired val bidRepository: BidReceiptRepository,
    @Autowired val saleRepository: SaleReceiptRepository,
    @Autowired val cancelRepository: CancelReceiptRepository
) : BidService {

    companion object {
        private val logger = LoggerFactory.getLogger(BidServiceImpl::class.java)
    }

    // for caching fetches
    private val taskExecutor = SimpleAsyncTaskExecutor()
    private var listAvailableArtworkResult: CopyOnWriteArrayList<AvailableArtwork>? = null

    override fun placeBid(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String) {
        logger.info("Processing bid $bidderName, $artworkId, $bidAmount, $currency in BidService")

        val bidReceipt = swapService.bidForArtwork(bidderName, artworkId, bidAmount, currency)

        bidRepository.store(bidReceipt)
    }

    override fun awardArtwork(bidderName: String, artworkId: ArtworkId, encumberedCurrency: String): List<Receipt> {
        logger.info("Processing award artwork $bidderName, $artworkId, $encumberedCurrency in BidService")

        val bidReceipt = bidRepository.retrieve(bidderName, artworkId, encumberedCurrency)

        val saleReceipt = swapService.awardArtwork(bidReceipt)
        saleRepository.store(saleReceipt)

        bidRepository.remove(bidderName, artworkId, encumberedCurrency)

        // cancel remaining bids
        val cancelReceipts = bidRepository.retrieveAllForId(artworkId).map { failedBid ->
            swapService.cancelBid(failedBid)
        }
        cancelReceipts.forEach {
            cancellationReceipt -> cancelRepository.store(cancellationReceipt)
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
        var initialLatch: CountDownLatch? = null
        if (this.listAvailableArtworkResult == null) {
            initialLatch = CountDownLatch(1)
        }

        taskExecutor.execute {
            val artworks = swapService.getAllArtworks()
            val resultList = artworks.map { artwork ->
                // get both bids and sales for target artwork
                val receiptsForId: List<Receipt> = (
                        bidRepository.retrieveAllForId(artwork.artworkId) +
                                saleRepository.retrieveAllForId(artwork.artworkId)
                        )
                val bidRecords = receiptsForId.map { receipt ->
                    val bidder = swapService.getPartyFromNameAndCurrency(receipt.bidderName, receipt.currency)
                    var cordaReference = ""
                    var accepted = false
                    when (receipt) {
                        is Receipt.BidReceipt -> { cordaReference = receipt.encumberedTokens }
                        is Receipt.SaleReceipt -> {
                            cordaReference = receipt.tokenTxId
                            accepted = true
                        }
                        else -> {}
                    }
                    AvailableArtwork.BidRecord(
                            cordaReference =  cordaReference,
                            bidderPublicKey = bidder.owningKey.hash.toString(),
                            bidderDisplayName = receipt.bidderName,
                            amountAndCurrency = Amount(receipt.amount, AuctionCurrency.getInstance(receipt.currency)),
                            notary = "${receipt.currency} Notary",
                            accepted = accepted
                    )
                }
                AvailableArtwork(
                        artworkId = artwork.artworkId,
                        description = artwork.description,
                        url = artwork.url,
                        listed = true,
                        expiryDate = Date.from(artwork.expiry),
                        bids = bidRecords
                )
            }
            this.listAvailableArtworkResult = CopyOnWriteArrayList(resultList)
            initialLatch?.let { initialLatch.countDown() }
        }
        initialLatch?.let { initialLatch.await() }
        return this.listAvailableArtworkResult!!
    }
}