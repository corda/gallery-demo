package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.service.ConnectionManager
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.workflows.RequestDraftTransferOfOwnershipFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.serialize
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class ArtNetworkBidderClientImpl(
    @Autowired private val connectionManager: ConnectionManager
) : ArtNetworkBidderClient {

    private lateinit var artNetworkBidderCS: ConnectionService

    // init client and set associated network
    @PostConstruct
    private fun postConstruct() {
        artNetworkBidderCS = connectionManager.auction
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkBidderClientImpl::class.java)
        private val network = CordaRPCNetwork.AUCTION
    }

    override fun issueTokens(bidderParty: TokenParty, amount: Long, currency: String) {
        logger.info("Starting IssueTokensFlow via $bidderParty for $amount $currency")
        artNetworkBidderCS.startFlow(bidderParty, IssueTokensFlow::class.java, amount, currency, bidderParty)
    }

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
        )

        with(validatedDraftTx) {
            return ValidatedUnsignedArtworkTransferTx(
                second.tx.serialize().bytes,
                second.controllingNotary.serialize().bytes,
                second.notarySignatureMetadata.serialize().bytes
            )
        }
    }
}