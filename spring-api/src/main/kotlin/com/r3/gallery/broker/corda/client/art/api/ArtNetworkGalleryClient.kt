package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.*
import java.time.Duration
import java.time.Instant

/**
 * Execute flows against Corda nodes running the Art Network application, acting as the gallery
 */
interface ArtNetworkGalleryClient {

    /**
     * Create a state representing ownership of the artwork with the id [artworkId], assigned to the gallery.
     *
     * - expiry is defaulted to 3 days from issuance.
     */
    fun issueArtwork(
        galleryParty: ArtworkParty,
        artworkId: ArtworkId,
        expiry: Instant? = Instant.now().plus(Duration.ofDays(3)),
        description: String = "",
        url: String = ""
    ): ArtworkOwnership

    /**
     * List out the artworks still held by the gallery.
     */
    fun listAvailableArtworks(galleryParty: ArtworkParty): List<ArtworkId>

    /**
     * Award an artwork to a bidder by signing and notarizing an unsigned art transfer transaction,
     * obtaining a [ProofOfTransferOfOwnership]
     *
     * @return Proof that ownership of the artwork has been transferred.
     */
    fun finaliseArtworkTransferTx(galleryParty: ArtworkParty, unsignedArtworkTransferTx: UnsignedArtworkTransferTx): ProofOfTransferOfOwnership

    /**
     * Get a representation of the ownership of the artwork with id [artworkId] by the gallery [galleryParty]
     */
    fun getOwnership(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership
}