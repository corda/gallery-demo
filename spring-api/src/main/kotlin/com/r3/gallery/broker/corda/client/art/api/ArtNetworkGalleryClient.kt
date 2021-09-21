package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.ArtworkOwnership
import com.r3.gallery.api.ProofOfTransferOfOwnership
import com.r3.gallery.api.UnsignedArtworkTransferTx
import com.r3.gallery.states.ArtworkState

import com.r3.gallery.api.*
import java.time.Duration
import java.time.Instant

/**
 * Execute flows against Corda nodes running the Art Network application, acting as the gallery
 */
interface ArtNetworkGalleryClient {

    /**
     * Issues an [ArtworkState] representing ownership of the artwork with the id [artworkId], assigned to the gallery.
     *
     * @param galleryParty who will issue/own the artwork
     * @param artworkId a unique UUID to identify the artwork by
     * @param expiry an [Instant] which will default to 3 days from 'now' if not provided
     * @param description of the artwork
     * @param url of the asset/img representing the artwork
     * @return [ArtworkOwnership]
     */
    fun issueArtwork(
        galleryParty: ArtworkParty,
        artworkId: ArtworkId,
        expiry: Instant? = Instant.now().plus(Duration.ofDays(3)),
        description: String = "",
        url: String = ""
    ): ArtworkOwnership

    /**
     * Award an artwork to a bidder by signing and notarizing an unsigned art transfer transaction,
     * obtaining a [ProofOfTransferOfOwnership]
     *
     * @param galleryParty who holds the artwork
     * @param unsignedArtworkTransferTx byte code representation of the transaction
     * @return [ProofOfTransferOfOwnership] that ownership of the artwork has been transferred.
     */
    fun finaliseArtworkTransferTx(galleryParty: ArtworkParty, unsignedArtworkTransferTx: UnsignedArtworkTransferTx): ProofOfTransferOfOwnership

    /**
     * Query a representation of the ownership of the artwork with id [artworkId] by the gallery [galleryParty]
     *
     * @param galleryParty who holds the artwork
     * @param artworkId
     * @return [ArtworkOwnership]
     */
    fun getOwnership(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership

    /**
     * Returns all available artwork states.
     * @return [List][ArtworkState]
     */
    fun getAllArtwork(): List<ArtworkState>
}
