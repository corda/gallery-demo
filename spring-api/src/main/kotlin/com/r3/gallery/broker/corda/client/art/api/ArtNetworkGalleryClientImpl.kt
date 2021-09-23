package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.utils.getNotaryTransactionSignature
import com.r3.gallery.workflows.SignAndFinalizeTransferOfOwnership
import com.r3.gallery.workflows.artwork.FindArtworkFlow
import com.r3.gallery.workflows.artwork.FindArtworksFlow
import com.r3.gallery.workflows.artwork.IssueArtworkFlow
import net.corda.core.concurrent.CordaFuture
import net.corda.core.messaging.startFlow
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 * Implementation of [ArtNetworkGalleryClient]
 */
@Component
class ArtNetworkGalleryClientImpl(
    @Autowired private val connectionManager: ConnectionManager
) : ArtNetworkGalleryClient {

    private lateinit var artNetworkGalleryCS: ConnectionService

    @PostConstruct
    private fun postConstruct() {
        artNetworkGalleryCS = connectionManager.auction
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkGalleryClientImpl::class.java)
    }

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
    override fun issueArtwork(galleryParty: ArtworkParty, artworkId: ArtworkId, expiry: Int, description: String, url: String): CordaFuture<ArtworkState> {
        logger.info("Starting IssueArtworkFlow via $galleryParty for $artworkId")
        val expInstant = Instant.now().plus(Duration.ofDays( 3))
        val state = artNetworkGalleryCS.startFlow(galleryParty, IssueArtworkFlow::class.java, artworkId, expInstant, description, url)

        return state.returnValue
    }

    /**
     * Award an artwork to a bidder by signing and notarizing an unsigned art transfer transaction,
     * obtaining a [ProofOfTransferOfOwnership]
     *
     * @param galleryParty who holds the artwork
     * @param unsignedArtworkTransferTx byte code representation of the transaction
     * @return [ProofOfTransferOfOwnership] that ownership of the artwork has been transferred.
     */
    override fun finaliseArtworkTransferTx(
        galleryParty: ArtworkParty,
        unsignedArtworkTransferTx: UnsignedArtworkTransferTx
    ): ProofOfTransferOfOwnership {
        logger.info("Starting SignAndFinalizeTransferOfOwnership flow via $galleryParty")
        val unsignedTx: WireTransaction =
            SerializedBytes<WireTransaction>(unsignedArtworkTransferTx.transactionBytes).deserialize()
        val proofOfTransfer: ProofOfTransferOfOwnership? =
            artNetworkGalleryCS.startFlow(galleryParty, SignAndFinalizeTransferOfOwnership::class.java, unsignedTx)
                    .returnValue.toCompletableFuture().thenApply {
                        ProofOfTransferOfOwnership(
                                transactionHash = it.id.toString(),
                                notarySignature = TransactionSignature(it.getNotaryTransactionSignature().serialize().bytes)
                        )
                    }.get(ConnectionServiceImpl.TIMEOUT, TimeUnit.SECONDS)
        return proofOfTransfer!!
    }

    /**
     * Get a representation of the ownership of the artwork with id [artworkId] by the gallery [galleryParty]
     *
     * @param galleryParty to search for ownership on
     * @param artworkId identifying the target art
     * @return [ArtworkOwnership]
     */
    override fun getOwnership(galleryParty: ArtworkParty, artworkId: ArtworkId): ArtworkOwnership {
        logger.info("Fetching ownership record for $galleryParty with artworkId: $artworkId")
        return galleryParty.artworkIdToState(artworkId).let {
            ArtworkOwnership(it.linearId.id, it.artworkId, it.owner.nameOrNull().toString())
        }
    }

    /**
     * Returns all available artwork states.
     * @return [List][ArtworkState]
     */
    override fun getAllArtwork(): List<ArtworkState> {
        logger.info("Retrieving all artwork on Auction Network")
        return artNetworkGalleryCS.allConnections()!!.flatMap {
            it.proxy.startFlow(::FindArtworksFlow).returnValue.get()
        }
    }

    /**
     * Query a representation of the ownership of the artwork with id [artworkId]
     *
     * @param artworkId
     * @return [ArtworkOwnership]
     */
    internal fun ArtworkParty.artworkIdToState(artworkId: ArtworkId): ArtworkState {
        logger.info("Fetching ArtworkState for artworkId $artworkId")
        return artNetworkGalleryCS.startFlow(this, FindArtworkFlow::class.java, artworkId).returnValue.get(
                ConnectionServiceImpl.TIMEOUT, TimeUnit.SECONDS
        )
    }
}