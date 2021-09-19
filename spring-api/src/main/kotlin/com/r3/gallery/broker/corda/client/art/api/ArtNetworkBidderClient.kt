package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx

/**
 * Execute flows against Corda nodes running the Art Network application, acting as a bidder
 */
interface ArtNetworkBidderClient {

    /**
     * TODO Move to AtomicSwapService
     * Used by bidder to request an unsigned draft transaction of the artwork transfer from gallery
     *
     * @param bidder of the artwork
     * @param gallery holding the artwork
     * @param artworkId represented the target artwork
     */
    fun requestDraftTransferOfOwnership(
        bidder: ArtworkParty,
        gallery: ArtworkParty,
        artworkId: ArtworkId
    ): ValidatedUnsignedArtworkTransferTx
}