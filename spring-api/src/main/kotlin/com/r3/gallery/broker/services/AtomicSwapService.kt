package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.TokenParty
import com.r3.gallery.broker.services.api.Receipt.*
import com.r3.gallery.states.ArtworkState
import net.corda.core.identity.Party

// TODO remove this hardcode (identity registry in atomic swap service should
const val GALLERY = "O=Alice, L=London, C=GB"

interface AtomicSwapService {
    fun bidForArtwork(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String): BidReceipt
    fun awardArtwork(bid: BidReceipt): SaleReceipt
    fun cancelBid(bid: BidReceipt): CancellationReceipt
    fun getAllArtworks(): List<ArtworkState>
    fun getPartyFromNameAndCurrency(buyerParty: TokenParty, currency: String): Party
}
