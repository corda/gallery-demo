package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.TokenParty

/**
 * Execute flows against Corda nodes running the Art Network application, acting as a bidder
 */
interface ArtNetworkBidderClient {
    fun issueTokens(bidderParty: TokenParty, amount: Long, currency: String = "GBP")
}