package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.broker.corda.client.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClient
import com.r3.gallery.broker.corda.client.config.ClientProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.*

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

    override suspend fun listAvailableArtworks(galleryParty: ArtworkParty): List<ArtworkId> {
        return listOf(UUID.randomUUID())
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