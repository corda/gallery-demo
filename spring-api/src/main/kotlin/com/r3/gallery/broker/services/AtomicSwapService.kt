package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.TokenParty
import com.r3.gallery.broker.services.api.Receipt.*
import com.r3.gallery.states.ArtworkState
import net.corda.core.concurrent.CordaFuture
import net.corda.core.identity.Party

// TODO remove this hardcode (identity registry in atomic swap service should
const val GALLERY = "O=Alice, L=London, C=GB"

/**
 * A service for cross-network atomic swaps.
 * @param galleryClient RPC client for the gallery node in the art network.
 * @param bidderClient RPC client for the bidder node in the art network.
 * @param buyerClient RPC client for the buyer node in the token networks (CBDC, GBO)
 * @param sellerClient RPC client for the seller node in the token networks (CBDC, GBO)
 * @param identityRegistry for known identities.
 */
interface AtomicSwapService {

    /**
     * The bidder bids for an artwork.
     * @param bidderName X500 name of the bidder.
     * @param artworkId the artwork to bid for.
     * @param bidAmount the bidder is willing to pay.
     * @param currency the bidder is paying with.
     * @return Details of the sale, with transaction ids for both legs of the swap.
     */
    fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String): BidReceipt

    /**
     * The gallery awards the artwork to the successful bid.
     * @param bid receipt of the winning bid.
     * @return Details of the sale, with transaction ids for both legs of the swap.
     */
    fun awardArtwork(bid: BidReceipt): SaleReceipt

    /**
     * The gallery cancel the losing bid, returning the offered tokens to the buyer.
     * @param bid receipt of the losing bid.
     * @return Details of the cancelled bid.
     */
    fun cancelBid(bid: BidReceipt): CancellationReceipt

    /**
     * Lists all artworks for sale by the gallery.
     */
    fun getAllArtworks(): List<CordaFuture<List<ArtworkState>>>

    /**
     * Resolves a [Party] from its name and currency.
     * @param buyerParty the X500 name of the party
     * @param currency network the party belongs to.
     */
    fun getPartyFromNameAndCurrency(buyerParty: TokenParty, currency: String): Party
}
