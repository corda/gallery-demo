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
     *
     * a. Request draft transaction from the Gallery on Auction network
     * b. Send encumbered tokens from Buyer to Seller on the Consideration network
     *
     * @param bidderName X500 name of the bidder.
     * @param artworkId the artwork to bid for.
     * @param bidAmount the bidder is willing to pay.
     * @param currency the bidder is paying with.
     * @return Details of the sale, with transaction ids for both legs of the swap.
     */
    fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String): BidReceipt

    /**
     * The gallery awards the artwork to the successful bid.
     *
     * a. Finalising the draft transaction (sign notarise the artwork transfer) by the Gallery on the Auction network.
     * b. Sending the notary signature of that transaction from the Gallery to it's counter-identity Seller on the
     * consideration network, then using that proof to claim the encumbered tokens.
     *
     * @param bid receipt of the winning bid.
     * @return Details of the sale, with transaction ids for both legs of the swap.
     */
    fun awardArtwork(bid: BidReceipt): SaleReceipt

    /**
     * The gallery cancel the losing bid, returning the offered tokens to the buyer.
     *
     * Note: This is a best-behavior action by the seller to release the lock if another bid has been accepted and finalised.
     * Should the seller refuse to initiate this action, the buyer is still protected in that the time-window on the
     * lock will allow them to initiate a claim after expiry.
     *
     * @param bid receipt of the losing bid.
     * @return Details of the cancelled bid.
     */
    fun cancelBid(bid: BidReceipt): CancellationReceipt

    /**
     * Returns all artwork across auction networks
     *
     * @return list of [CordaFuture] representing the artworks held by each gallery
     */
    fun getAllArtworks(): List<CordaFuture<List<ArtworkState>>>

    /**
     * Resolves a [Party] from its name and currency.
     *
     * @param buyerParty the X500 name of the party
     * @param currency network the party belongs to.
     * @return [Party]
     */
    fun getPartyFromNameAndCurrency(buyerParty: TokenParty, currency: String): Party
}
