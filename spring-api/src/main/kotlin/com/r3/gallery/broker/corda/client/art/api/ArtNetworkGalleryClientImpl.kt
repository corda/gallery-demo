package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.utils.getNotaryTransactionSignature
import com.r3.gallery.workflows.SignAndFinalizeTransferOfOwnership
import com.r3.gallery.workflows.artwork.FindArtworkFlow
import com.r3.gallery.workflows.artwork.FindOwnedArtworksFlow
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import net.corda.core.internal.toX500Name
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class ArtNetworkGalleryClientImpl(
    @Autowired private val connectionManager: ConnectionManager
) : ArtNetworkGalleryClient {

    private lateinit var artNetworkGalleryCS: ConnectionService

    // init client and set associated network
    @PostConstruct
    private fun postConstruct() {
        artNetworkGalleryCS = connectionManager.auction
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkGalleryClientImpl::class.java)
    }

    /**
     * Create a state representing ownership of the artwork with the id [artworkId], assigned to the gallery.
     */
    override fun issueArtwork(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership {
        logger.info("Starting IssueArtworkFlow via $galleryParty for $artworkId")
        val state = artNetworkGalleryCS.startFlow(galleryParty, IssueArtworkFlow::class.java, artworkId)
        return ArtworkOwnership(
            state.linearId.id,
            state.artworkId,
            state.owner.nameOrNull()!!.toX500Name().toString()
        )
    }

    /**
     * List out the artworks still held by the gallery.
     */
    override fun listAvailableArtworks(galleryParty: ArtworkParty): List<ArtworkId> {
        logger.info("Starting ListAvailableArtworks flow via $galleryParty")
        return artNetworkGalleryCS.startFlow(galleryParty, FindOwnedArtworksFlow::class.java)
            .map { it.state.data.artworkId }
    }

    /**
     * Award an artwork to a bidder by signing and notarizing an unsigned art transfer transaction,
     * obtaining a [ProofOfTransferOfOwnership]
     *
     * @return Proof that ownership of the artwork has been transferred.
     */
    override fun finaliseArtworkTransferTx(
        galleryParty: ArtworkParty,
        unsignedArtworkTransferTx: UnsignedArtworkTransferTx
    ): ProofOfTransferOfOwnership {
        logger.info("Starting SignAndFinalizeTransferOfOwnership flow via $galleryParty")
        val unsignedTx: WireTransaction =
            SerializedBytes<WireTransaction>(unsignedArtworkTransferTx.transactionBytes).deserialize()
        val signedTx: SignedTransaction =
            artNetworkGalleryCS.startFlow(galleryParty, SignAndFinalizeTransferOfOwnership::class.java, unsignedTx)
        return ProofOfTransferOfOwnership(
            transactionHash = signedTx.id.toString(),
            notarySignature = TransactionSignature(signedTx.getNotaryTransactionSignature().serialize().bytes)
        )
    }

    /**
     * Get a representation of the ownership of the artwork with id [artworkId] by the gallery [galleryParty]
     */
    override fun getOwnership(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership {
        logger.info("Fetching ownership record for $galleryParty with artworkId: $artworkId")
        return galleryParty.artworkIdToState(artworkId).let {
            ArtworkOwnership(it.linearId.id, it.artworkId, it.owner.nameOrNull().toString())
        }
    }

    /**
     * Returns the ArtworkState associated with the ArtworkId
     */
    internal fun ArtworkParty.artworkIdToState(artworkId: ArtworkId): ArtworkState {
        logger.info("Fetching ArtworkState for artworkId $artworkId")
        return artNetworkGalleryCS.startFlow(this, FindArtworkFlow::class.java, artworkId)
    }
}