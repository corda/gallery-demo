package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.UnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClient
import com.r3.gallery.broker.services.api.Receipt
import com.r3.gallery.broker.services.api.Receipt.*
import com.r3.gallery.states.ArtworkState
import net.corda.core.concurrent.CordaFuture
import net.corda.core.identity.Party
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Implementation of [AtomicSwapService]
 */
@Component
class AtomicSwapServiceImpl(
        @Autowired val galleryClient: ArtNetworkGalleryClient,
        @Autowired val bidderClient: ArtNetworkBidderClient,
        @Autowired val buyerClient: TokenNetworkBuyerClient,
        @Autowired val sellerClient: TokenNetworkSellerClient,
        @Autowired val identityRegistry: IdentityRegistry
) : AtomicSwapService {

    private val galleryParty get() = identityRegistry.getArtworkParty(GALLERY)
    private val sellerParty get() = identityRegistry.getTokenParty(GALLERY)

    /**
     * Creates a bid for artwork through a Pre-Setup phase of atomic swap.
     *
     * a. Request draft transaction from the Gallery on Auction network
     * b. Send encumbered tokens from Buyer to Seller on the Consideration network
     *
     * @param bidderName x500 name of a valid bidder on the Auction network
     * @param artworkId the unique identifier of the artwork being bid on
     * @param bidAmount the amount to bid in Long (conversions from decimal are executed prior to this call)
     * @param currency used to map the bid to the correct consideration network
     * @return [BidReceipt]
     */
    override fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String): BidReceipt {

        val bidderParty = identityRegistry.getArtworkParty(bidderName)
        val buyerParty = identityRegistry.getTokenParty(bidderName)

        val ownership = galleryClient.getOwnership(galleryParty, artworkId)
        val validatedUnsignedTx =
            bidderClient.requestDraftTransferOfOwnership(bidderParty, galleryParty, ownership.cordaReference)
        val encumberedTokens =
            buyerClient.transferEncumberedTokens(buyerParty, sellerParty, bidAmount, currency, validatedUnsignedTx)

        val unsignedArtworkTransferTx = UnsignedArtworkTransferTx(validatedUnsignedTx.transactionBytes)

        return BidReceipt(
                bidderName,
                artworkId,
                bidAmount,
                currency,
                unsignedArtworkTransferTx,
                encumberedTokens)
    }

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
    override fun awardArtwork(bid: BidReceipt): SaleReceipt {
        val proofOfTransfer = galleryClient.finaliseArtworkTransferTx(galleryParty, bid.unsignedArtworkTransferTx)
        val tokenTxId = sellerClient.claimTokens(sellerParty, bid.currency, bid.encumberedTokens, proofOfTransfer.notarySignature)

        return SaleReceipt(bid.bidderName, bid.artworkId, bid.amount, bid.currency, proofOfTransfer.transactionHash, tokenTxId)
    }

    /**
     * Executes a pre-(time-window expiry) release of encumbered tokens back to the original holder.
     * Note: This is a best-behavior action by the seller to release the lock if another bid has been accepted and finalised.
     * Should the seller refuse to initiate this action, the buyer is still protected in that the time-window on the
     * lock will allow them to initiate a claim after expiry.
     *
     * @param bid represented by a [BidReceipt]
     * @return [CancellationReceipt] with details of the tx
     */
    override fun cancelBid(bid: BidReceipt): CancellationReceipt {

        val tokenTxId = sellerClient.releaseTokens(
            sellerParty,
            bid.currency,
            bid.encumberedTokens)

        return CancellationReceipt(bid.bidderName, bid.artworkId, bid.amount, bid.currency, tokenTxId)
    }

    /**
     * Returns all artwork across auction networks
     *
     * @return list of [CordaFuture] representing the artworks held by each gallery
     */
    override fun getAllArtworks(): List<CordaFuture<List<ArtworkState>>> {
        return galleryClient.getAllArtwork()
    }

    /**
     * Returns a Buyer [Party] object retrieved from a consideration network.
     *
     * @return [Party]
     */
    override fun getPartyFromNameAndCurrency(buyerParty: TokenParty, currency: String): Party {
        return buyerClient.resolvePartyFromNameAndCurrency(buyerParty, currency)
    }
}