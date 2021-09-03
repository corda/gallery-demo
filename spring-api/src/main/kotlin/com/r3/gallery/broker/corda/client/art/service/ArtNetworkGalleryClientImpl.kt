package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.config.ClientProperties
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.webapp.artnetwork.gallery.CreateArtworkTransferTx
import com.r3.gallery.workflows.webapp.artnetwork.gallery.FinaliseArtworkTransferTx
import com.r3.gallery.workflows.webapp.artnetwork.gallery.IssueArtworkFlow
import com.r3.gallery.workflows.webapp.artnetwork.gallery.ListAvailableArtworks
import com.r3.gallery.workflows.webapp.artnetwork.gallery.utilityflows.ArtworkIdToState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ArtNetworkGalleryClientImpl(
    @Autowired
    @Qualifier("ArtNetworkGalleryProperties")
    clientProperties: ClientProperties
) : NodeClient(clientProperties), ArtNetworkGalleryClient {

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkGalleryClientImpl::class.java)
    }

    /**
     * Create a state representing ownership of the artwork with the id [artworkId], assigned to the gallery.
     */
    override fun issueArtwork(galleryParty: ArtworkParty, artworkId: ArtworkId) : ArtworkOwnership {
        logger.info("Starting IssueArtworkFlow via $galleryParty for $artworkId")
        return galleryParty.network().startFlow(IssueArtworkFlow::class.java, artworkId)
    }

    /**
     * List out the artworks still held by the gallery.
     */
    override fun listAvailableArtworks(galleryParty: ArtworkParty): List<ArtworkId> {
        logger.info("Starting ListAvailableArtworks flow via $galleryParty")
        return galleryParty.network().startFlow(ListAvailableArtworks::class.java, galleryParty)
    }

    /**
     * Create an unsigned transaction that would transfer an artwork owned by the gallery,
     * identified by [galleryOwnership], to the given bidder.
     *
     * @return The unsigned fulfilment transaction
     */
    override fun createArtworkTransferTx(galleryParty: ArtworkParty, bidderParty: ArtworkParty, galleryOwnership: ArtworkOwnership): UnsignedArtworkTransferTx {
        logger.info("Starting CreateArtworkTransferTx flow via $galleryParty with bidder: $bidderParty for ownership $galleryOwnership")
        return galleryParty.network().startFlow(CreateArtworkTransferTx::class.java, bidderParty, galleryOwnership)
    }
    /**
     * Award an artwork to a bidder by signing and notarizing an unsigned art transfer transaction,
     * obtaining a [ProofOfTransferOfOwnership]
     *
     * @return Proof that ownership of the artwork has been transferred.
     */
    override fun finaliseArtworkTransferTx(galleryParty: ArtworkParty, unsignedArtworkTransferTx: UnsignedArtworkTransferTx): ProofOfTransferOfOwnership {
        logger.info("Starting FinaliseArtworkTransferTx flow via $galleryParty for $unsignedArtworkTransferTx")
        return galleryParty.network().startFlow(FinaliseArtworkTransferTx::class.java, unsignedArtworkTransferTx)
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
     * Simple shorthand for describing connection id in terms of node vs network
     */
    internal fun ArtworkParty.network() : RpcConnectionTarget
        = (this + CordaRPCNetwork.AUCTION.toString())
            .also { idExists(it) } // check validity

    /**
     * Returns the ArtworkState associated with the ArtworkId
     */
    internal fun ArtworkParty.artworkIdToState(artworkId: ArtworkId): ArtworkState {
        logger.info("Fetching ArtworkState for artworkId $artworkId")
        return network().startFlow(ArtworkIdToState::class.java, artworkId)
    }

    /**
     * Returns the ArtworkState associated with the CordaReference
     */
    internal fun ArtworkParty.artworkIdToCordaReference(artworkId: ArtworkId): CordaReference {
        return artworkIdToState(artworkId).linearId.id
    }
}