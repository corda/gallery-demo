package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.TokenParty
import com.r3.gallery.broker.services.api.Receipt
import com.r3.gallery.broker.services.api.Receipt.*
import com.r3.gallery.states.ArtworkState
import net.corda.core.concurrent.CordaFuture
import net.corda.core.identity.Party

// TODO remove this hardcode (identity registry in atomic swap service should
const val GALLERY = "O=Alice, L=London, C=GB"

/**
 * Executes phases of a cross-network atomic swap through orchestrated multistage transactions via RPC.
 * These operations are initiated by the [BidService]
 */
interface AtomicSwapService {

    /**
     * Creates a bid for artwork through a Pre-Setup phase of atomic swap.
     *
     * a. Request draft transaction of transfer of artwork from the Gallery on Auction network
     * b. Send encumbered tokens from Buyer to Seller on the Consideration network
     *
     * @param bidderName x500 name of a valid bidder on the Auction network
     * @param artworkId the unique identifier of the artwork being bid on
     * @param bidAmount the amount to bid in Long (conversions from decimal are executed prior to this call)
     * @param currency used to map the bid to the correct consideration network
     * @return [BidReceipt]
     */
    fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String): BidReceipt

    /**
     * Awards an artwork by completion of a Proof-of-Action phase of the atomic swap.
     *
     * a. Finalising the draft transaction (sign notarise the artwork transfer) by the Gallery on the Auction network.
     * b. Sending the notary signature of that transaction from the Gallery to it's counter-identity Seller on the
     * consideration network, then using that proof to claim the encumbered tokens.
     *
     * @param bid represented by a [BidReceipt]
     * @return [SaleReceipt] with details of the tx
     */
    fun awardArtwork(bid: BidReceipt): SaleReceipt

    /**
     * Executes a pre-(time-window expiry) release of encumbered tokens back to the original holder.
     * Note: This is a best-behavior action by the seller to release the lock if another bid has been accepted and finalised.
     * Should the seller refuse to initiate this action, the buyer is still protected in that the time-window on the
     * lock will allow them to initiate a claim after expiry.
     *
     * @param bid represented by a [BidReceipt]
     * @return [CancellationReceipt] with details of the tx
     */
    fun cancelBid(bid: BidReceipt): CancellationReceipt

    /**
     * Returns all artwork across auction networks
     *
     * @return list of [CordaFuture] representing the artworks held by each gallery
     */
    fun getAllArtworks(): List<CordaFuture<List<ArtworkState>>>

    /**
     * Returns a Buyer [Party] object retrieved from a consideration network.
     *
     * @return [Party]
     */
    fun getPartyFromNameAndCurrency(buyerParty: TokenParty, currency: String): Party
}
