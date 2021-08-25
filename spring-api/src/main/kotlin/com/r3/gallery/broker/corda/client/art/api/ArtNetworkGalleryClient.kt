package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.broker.corda.client.api.*
import com.r3.gallery.other.ArtworkId
import com.r3.gallery.other.ArtworkOwnership
import com.r3.gallery.other.ArtworkParty
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort


/**
 * Execute flows against Corda nodes running the Art Network application, acting as the gallery
 */
interface ArtNetworkGalleryClient {

    /**
     * Create a state representing ownership of the artwork with the id [artworkId], assigned to the gallery.
     */
    suspend fun issueArtwork(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership {
        val nodeAddress: NetworkHostAndPort = NetworkHostAndPort.parse(args.get(0))
        val username: String = args.get(1)
        val password: String = args.get(2)

        val client: net.corda.client.rpc.CordaRPCClient = net.corda.client.rpc.CordaRPCClient(nodeAddress)
        val connection: net.corda.client.rpc.CordaRPCConnection = client.start(username, password)
        val cordaRPCOperations: CordaRPCOps = connection.proxy
    }

    /**
     * List out the artworks still held by the gallery.
     */
    suspend fun listAvailableArtworks(galleryParty: ArtworkParty): List<ArtworkId>

    /**
     * Create an unsigned transaction that would transfer an artwork owned by the gallery,
     * identified by [galleryOwnership], to the given [bidder].
     *
     * @return The unsigned fulfilment transaction
     */
    suspend fun createArtworkTransferTx(galleryPart: ArtworkParty,
                                        bidderParty: ArtworkParty,
                                        galleryOwnership: ArtworkOwnership): UnsignedArtworkTransferTx {


    }

    /**
     * Award an artwork to a bidder by signing and notarizing an unsigned art transfer transaction,
     * obtaining a [ProofOfTransferOfOwnership]
     *
     * @return Proof that ownership of the artwork has been transferred.
     */
    suspend fun finaliseArtworkTransferTx(galleryParty: ArtworkParty, unsignedArtworkTransferTx: UnsignedArtworkTransferTx): ProofOfTransferOfOwnership

    /**
     * Get a representation of the ownership of the artwork with id [artworkId] by the gallery [galleryParty]
     */
    suspend fun getOwnership(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership
}