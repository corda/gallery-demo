package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.VerifiedUnsignedArtworkTransferTx

/**
 * Execute flows against Corda nodes running the Art Network application, acting as a bidder
 */
interface ArtNetworkBidderClient {
    fun issueTokens(bidderParty: TokenParty, amount: Long, currency: String = "GBP")

    fun requestDraftTransferOfOwnership(
        bidderParty: ArtworkParty,
        galleryParty: ArtworkParty,
        artworkId: ArtworkId
    ): VerifiedUnsignedArtworkTransferTx
}