package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.broker.corda.client.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.config.ClientProperties
import com.r3.gallery.states.AuctionState
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

    override suspend fun issueArtwork(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership {
        TODO("Not yet implemented")
    }

    /**
     * Returns a list of available artworks for bidding
     * - artworkIds that are tied to an unconsumed auction state
     */
    override suspend fun listAvailableArtworks(galleryParty: ArtworkParty): List<ArtworkId> {
        return  execute(galleryParty idOn CordaRPCNetwork.AUCTION.toString()) { connection ->
            connection.proxy.vaultQuery(AuctionState::class.java)
        }.states.map { it.state.data.artworkId }.distinct()
    }

    override suspend fun createArtworkTransferTx(galleryPart: ArtworkParty, bidderParty: ArtworkParty, galleryOwnership: ArtworkOwnership): UnsignedArtworkTransferTx {
        TODO("Not yet implemented")
    }

    override suspend fun finaliseArtworkTransferTx(galleryParty: ArtworkParty, unsignedArtworkTransferTx: UnsignedArtworkTransferTx): ProofOfTransferOfOwnership {
        TODO("Not yet implemented")
    }

    override suspend fun getOwnership(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership {
        TODO("Not yet implemented")
    }
}