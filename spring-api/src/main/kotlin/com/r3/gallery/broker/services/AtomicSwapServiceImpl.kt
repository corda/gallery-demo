package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.UnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClient
import com.r3.gallery.broker.services.api.Receipt
import com.r3.gallery.states.ArtworkState
import net.corda.core.concurrent.CordaFuture
import net.corda.core.identity.Party
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * A service for cross-network atomic swaps.
 * @param galleryClient RPC client for the gallery node in the art network.
 * @param bidderClient RPC client for the bidder node in the art network.
 * @param buyerClient RPC client for the buyer node in the token networks (CBDC, GBO)
 * @param sellerClient RPC client for the seller node in the token networks (CBDC, GBO)
 * @param identityRegistry for known identities.
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
     * The bidder bids for an artwork.
     * @param bidderName X500 name of the bidder.
     * @param artworkId the artwork to bid for.
     * @param bidAmount the bidder is willing to pay.
     * @param currency the bidder is paying with.
     * @return Details of the sale, with transaction ids for both legs of the swap.
     */
    override fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String): Receipt.BidReceipt {

        val bidderParty = identityRegistry.getArtworkParty(bidderName)
        val buyerParty = identityRegistry.getTokenParty(bidderName)

        val ownership = galleryClient.getOwnership(galleryParty, artworkId)
        val validatedUnsignedTx =
            // TODO: see HACK in RequestDraftTransferOfOwnershipFlow.kt to workaround the inconsistency in use
            //       of CordaReference vs ArtworkId along the chain of calls.
            bidderClient.requestDraftTransferOfOwnership(bidderParty, galleryParty, ownership.cordaReference)
        val encumberedTokens =
            buyerClient.transferEncumberedTokens(buyerParty, sellerParty, bidAmount, currency, validatedUnsignedTx)

        val unsignedArtworkTransferTx = UnsignedArtworkTransferTx(validatedUnsignedTx.transactionBytes)

        return Receipt.BidReceipt(
                bidderName,
                artworkId,
                bidAmount,
                currency,
                unsignedArtworkTransferTx,
                encumberedTokens)
    }

    /**
     * The gallery awards the artwork to the successful bid.
     * @param bid receipt of the winning bid.
     * @return Details of the sale, with transaction ids for both legs of the swap.
     */
    override fun awardArtwork(bid: Receipt.BidReceipt): Receipt.SaleReceipt {
        val proofOfTransfer = galleryClient.finaliseArtworkTransferTx(galleryParty, bid.unsignedArtworkTransferTx)
        val tokenTxId = sellerClient.claimTokens(sellerParty, bid.currency, bid.encumberedTokens, proofOfTransfer.notarySignature)

        return Receipt.SaleReceipt(bid.bidderName, bid.artworkId, bid.amount, bid.currency, proofOfTransfer.transactionHash, tokenTxId)
    }

    /**
     * The gallery cancel the losing bid, returning the offered tokens to the buyer.
     * @param bid receipt of the losing bid.
     * @return Details of the cancelled bid.
     */
    override fun cancelBid(bid: Receipt.BidReceipt): Receipt.CancellationReceipt {

        val tokenTxId = sellerClient.releaseTokens(
            sellerParty,
            bid.currency,
            bid.encumberedTokens)

        return Receipt.CancellationReceipt(bid.bidderName, bid.artworkId, bid.amount, bid.currency, tokenTxId)
    }

    /**
     * Lists all artworks for sale by the gallery.
     */
    override fun getAllArtworks(): List<CordaFuture<List<ArtworkState>>> {
        return galleryClient.getAllArtwork()
    }

    /**
     * Resolves a [Party] from its name and currency.
     * @param buyerParty the X500 name of the party
     * @param currency network the party belongs to.
     */
    override fun getPartyFromNameAndCurrency(buyerParty: TokenParty, currency: String): Party {
        return buyerClient.resolvePartyFromNameAndCurrency(buyerParty, currency)
    }
}