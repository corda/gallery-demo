package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.AvailableArtwork
import com.r3.gallery.broker.services.api.Receipt
import java.util.concurrent.CompletableFuture

interface BidService {
    /**
     * Places a bid initiated by a Bidder and stores a bid receipt in a [ReceiptRepository]
     *
     * @param bidderName x500 name of a valid bidder on the Auction network
     * @param artworkId the unique identifier of the artwork being bid on
     * @param bidAmount the amount to bid in Long (conversions from decimal are executed prior to this call)
     * @param currency used to map the bid to the correct consideration network
     */
    fun placeBid(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String)

    /**
     * Awards an artwork, initiated by a Gallery, stores a sales receipt (for accepted bid),
     * and set of cancellation receipts (for all non-accepted bids).
     *
     * @param bidderName x500 name of the bidder whose bid is being accepted
     * @param artworkId the unique identifier of the artwork to be transferred/sold
     * @param encumberedCurrency the currency of the bid which was placed
     * @return [List][Receipt] a receipt summary of the auction including sale and cancellation receipts.
     */
    fun awardArtwork(bidderName: String, artworkId: ArtworkId, encumberedCurrency: String): List<Receipt>

    /**
     * Returns a list of futures referencing a list of available artworks (later transformed to List<AvailableArtwork>)
     * Receipts for bids on the artwork are embedded in the result.
     *
     * @param galleryParty the party whose RPC connection initiates the query
     * @return list of [CompletableFuture]
     */
    fun listAvailableArtworks(galleryParty: ArtworkParty): List<CompletableFuture<List<AvailableArtwork>>>
}
