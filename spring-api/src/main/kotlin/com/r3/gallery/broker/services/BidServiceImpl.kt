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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of [BidService]
 */
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

    private val listAvailableArtworkCache: MutableList<CompletableFuture<List<AvailableArtwork>>> = CopyOnWriteArrayList()

    /**
     * Places a bid initiated by a Bidder and stores a receipt in a [ReceiptRepository]
     *
     * @param bidderName x500 name of a valid bidder on the Auction network
     * @param artworkId the unique identifier of the artwork being bid on
     * @param bidAmount the amount to bid in Long (conversions from decimal are executed prior to this call)
     * @param currency used to map the bid to the correct consideration network
     */
    override fun placeBid(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String) {
        logger.info("Processing bid $bidderName, $artworkId, $bidAmount, $currency in BidService")

        val bidReceipt = swapService.bidForArtwork(bidderName, artworkId, bidAmount, currency)

        bidRepository.store(bidReceipt)
    }

    /**
     * Awards an artwork, initiated by a Gallery, stores a sales receipt (for accepted bid),
     * and set of cancellation receipts (for all non-accepted bids).
     *
     * @param bidderName x500 name of the bidder whose bid is being accepted
     * @param artworkId the unique identifier of the artwork to be transferred/sold
     * @param encumberedCurrency the currency of the bid which was placed
     * @return [List][Receipt] a receipt summary of the auction including sale and cancellation receipts.
     */
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
        cancelReceipts.forEach { cancellationReceipt ->
            cancelRepository.store(cancellationReceipt)
        }

        return listOf(saleReceipt) + cancelReceipts
    }

    /**
     * Returns a list of futures referencing a list of available artworks (later transformed to List<AvailableArtwork>
     * Receipts for bids on the artwork are embedded in the result.
     *
     * @param galleryParty the party whose RPC connection initiates the query
     * @return list of [CompletableFuture]
     */
    override fun listAvailableArtworks(galleryParty: ArtworkParty): List<CompletableFuture<List<AvailableArtwork>>> {
        logger.info("Listing available artworks via $galleryParty")
        return if (listAvailableArtworkCache.isNotEmpty()) listAvailableArtworkCache.also {
            updateArtworkCache()
        } else updateArtworkCache().let { listAvailableArtworkCache }
    }

    /** Helper function to update n-1 result cache for faster polling response */
    private fun updateArtworkCache() {
        val artworks = swapService.getAllArtworks()
        val completableFutureArtworkList = artworks.map { artworkFuture -> artworkFuture.toCompletableFuture() }

        listAvailableArtworkCache.clear()
        listAvailableArtworkCache.addAll(completableFutureArtworkList.map { completableFuture ->
            completableFuture.thenApplyAsync { artworks ->
                artworks.map { artwork ->
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
                            is Receipt.BidReceipt -> {
                                cordaReference = receipt.encumberedTokens
                            }
                            is Receipt.SaleReceipt -> {
                                cordaReference = receipt.tokenTxId
                                accepted = true
                            }
                            else -> {
                            }
                        }
                        AvailableArtwork.BidRecord(
                                cordaReference = cordaReference,
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
            }
        })
    }
}