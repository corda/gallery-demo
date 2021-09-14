package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.workflows.RequestDraftTransferOfOwnershipFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.serialize
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class ArtNetworkBidderClientImpl : ArtNetworkBidderClient {

    private lateinit var artNetworkBidderCS: ConnectionService

    @Autowired
    @Qualifier("ArtNetworkBidderProperties")
    private lateinit var artNetworkBidderProperties: ClientProperties

    // init client and set associated network
    @PostConstruct
    private fun postConstruct() {
        artNetworkBidderCS = ConnectionServiceImpl(artNetworkBidderProperties)
        artNetworkBidderCS.associatedNetwork = network
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
                tx.serialize().bytes,
                controllingNotary.serialize().bytes,
                notarySignatureMetadata.serialize().bytes
            )
        }
    }
}