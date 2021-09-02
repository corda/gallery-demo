package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.ArtworkParty

/**
 * Execute flows against Corda nodes running the Art Network application, acting as a bidder
 */
interface ArtNetworkBidderClient {
    suspend fun issueTokens(bidderParty: ArtworkParty, amount: Long, currency: String = "GBP"): Unit
}