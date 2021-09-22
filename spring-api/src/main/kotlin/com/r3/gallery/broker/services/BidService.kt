package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.AvailableArtwork
import com.r3.gallery.broker.services.api.Receipt

interface BidService {
    fun placeBid(bidderName: String, artworkId: ArtworkId, bidAmount: Long, currency: String)
    fun awardArtwork(bidderName: String, artworkId: ArtworkId, encumberedCurrency: String): List<Receipt>
    fun listAvailableArtworks(galleryParty: ArtworkParty): List<AvailableArtwork>
}
