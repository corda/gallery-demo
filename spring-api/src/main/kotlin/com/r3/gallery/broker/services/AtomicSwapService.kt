package com.r3.gallery.broker.services

import com.r3.gallery.broker.corda.client.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkSellerClient
import com.r3.gallery.broker.services.api.Receipt.*
import org.springframework.beans.factory.annotation.Autowired

const val GALLERY = "gallery"

open class AtomicSwapService(
    @Autowired val galleryClient: ArtNetworkGalleryClient,
    @Autowired val buyerClient: TokenNetworkBuyerClient,
    @Autowired val sellerClient: TokenNetworkSellerClient,
    @Autowired val identityRegistry: IdentityRegistry
) {

    private val galleryParty = identityRegistry.getArtworkParty(GALLERY)
    private val sellerParty = identityRegistry.getTokenParty(GALLERY)

    suspend fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Int): BidReceipt {
        val bidderParty = identityRegistry.getArtworkParty(bidderName)
        val buyerParty = identityRegistry.getTokenParty(bidderName)

        val ownership = galleryClient.getOwnership(galleryParty, artworkId)
        val unsignedTx = galleryClient.createArtworkTransferTx(galleryParty, bidderParty, ownership)
        val encumberedTokens = buyerClient.transferEncumberedTokens(buyerParty, sellerParty, bidAmount, unsignedTx)

        return BidReceipt(bidderName, artworkId, unsignedTx, encumberedTokens)
    }

    /**
     * The gallery awards the artwork to the successful bid.
     *
     * @return Details of the sale, with transaction ids for both legs of the swap.
     */
    suspend fun awardArtwork(bid: BidReceipt): SaleReceipt {
        val proofOfTransfer = galleryClient.finaliseArtworkTransferTx(galleryParty, bid.unsignedArtworkTransferTx)
        val tokenTxId = sellerClient.claimTokens(sellerParty, bid.encumberedTokens, proofOfTransfer)

        return SaleReceipt(bid.bidderName, bid.artworkId, proofOfTransfer.transactionId, tokenTxId)
    }

    suspend fun cancelBid(bid: BidReceipt): CancellationReceipt {
        val tokenTxId = sellerClient.releaseTokens(
            sellerParty,
            identityRegistry.getTokenParty(bid.bidderName),
            bid.encumberedTokens)

        return CancellationReceipt(bid.bidderName, bid.artworkId, tokenTxId)
    }
}