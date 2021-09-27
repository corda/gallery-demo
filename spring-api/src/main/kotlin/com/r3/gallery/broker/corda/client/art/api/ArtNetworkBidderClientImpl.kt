package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.ValidatedUnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.workflows.RequestDraftTransferOfOwnershipFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.serialize
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 * An implementation of [ArtNetworkBidderClient]
 */
@Component
class ArtNetworkBidderClientImpl(
    @Autowired private val connectionManager: ConnectionManager
) : ArtNetworkBidderClient {

    private lateinit var artNetworkBidderCS: ConnectionService

    /** Initialize client */
    @PostConstruct
    private fun postConstruct() {
        artNetworkBidderCS = connectionManager.auction
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkBidderClientImpl::class.java)
    }

    /**
     * TODO Move to AtomicSwapService
     * Used by bidder to request an unsigned draft transaction of the artwork transfer from gallery
     *
     * @param bidder of the artwork
     * @param gallery holding the artwork
     * @param artworkId represented the target artwork
     */
    override fun requestDraftTransferOfOwnership(
        bidder: ArtworkParty,
        gallery: ArtworkParty,
        artworkId: ArtworkId
    ): ValidatedUnsignedArtworkTransferTx {
        logger.info("Starting RequestDraftTransferOfOwnershipFlow via $bidder for $artworkId from $gallery")

        val galleryParty = artNetworkBidderCS.wellKnownPartyFromName(bidder, gallery)

        val validatedDraftTx = artNetworkBidderCS.startFlow(
            bidder,
            RequestDraftTransferOfOwnershipFlow::class.java,
            galleryParty,
            UniqueIdentifier.fromString(artworkId.toString())
        ).returnValue.toCompletableFuture().thenApply {
            ValidatedUnsignedArtworkTransferTx(
                    it.tx.serialize().bytes,
                    it.controllingNotary.serialize().bytes,
                    it.notarySignatureMetadata.serialize().bytes
            )
        }

        return  validatedDraftTx.get(ConnectionServiceImpl.TIMEOUT, TimeUnit.SECONDS)
    }
}