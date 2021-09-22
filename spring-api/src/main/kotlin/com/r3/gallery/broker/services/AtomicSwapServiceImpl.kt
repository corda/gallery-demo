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
import net.corda.core.identity.Party
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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

    override fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String): Receipt.BidReceipt {

        val bidderParty = identityRegistry.getArtworkParty(bidderName)
        val buyerParty = identityRegistry.getTokenParty(bidderName)

        val ownership = galleryClient.getOwnership(galleryParty, artworkId)
        val validatedUnsignedTx =
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
     *
     * @return Details of the sale, with transaction ids for both legs of the swap.
     */
    override fun awardArtwork(bid: Receipt.BidReceipt, currency: String): Receipt.SaleReceipt {
        val proofOfTransfer = galleryClient.finaliseArtworkTransferTx(galleryParty, bid.unsignedArtworkTransferTx)
        val tokenTxId = sellerClient.claimTokens(sellerParty, currency, bid.encumberedTokens, proofOfTransfer.notarySignature)

        return Receipt.SaleReceipt(bid.bidderName, bid.artworkId, bid.amount, bid.currency, proofOfTransfer.transactionHash, tokenTxId)
    }

    override fun cancelBid(bid: Receipt.BidReceipt, currency: String): Receipt.CancellationReceipt {

        val tokenTxId = sellerClient.releaseTokens(
            sellerParty,
            currency,
            bid.encumberedTokens)

        return Receipt.CancellationReceipt(bid.bidderName, bid.artworkId, bid.amount, bid.currency, tokenTxId)
    }

    override fun getAllArtworks(): List<ArtworkState> {
        return galleryClient.getAllArtwork()
    }

    override fun getPartyFromNameAndCurrency(buyerParty: TokenParty, currency: String): Party {
        return buyerClient.resolvePartyFromNameAndCurrency(buyerParty, currency)
    }
}