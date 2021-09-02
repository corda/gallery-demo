package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.config.ClientProperties
import com.r3.gallery.states.ArtworkState
import com.r3.gallery.workflows.artwork.FindArtworkFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ArtNetworkBidderClientImpl(
    @Autowired
    @Qualifier("ArtNetworkBidderProperties")
    clientProperties: ClientProperties
) : NodeClient(clientProperties), ArtNetworkBidderClient {

    companion object {
        private val logger = LoggerFactory.getLogger(ArtNetworkGalleryClientImpl::class.java)
    }

    override suspend fun issueTokens(bidderParty: TokenParty, amount: Long, currency: String) {
        bidderParty.network().startFlow(IssueTokensFlow::class.java, amount, currency, bidderParty)
    }

    // TODO: review for multiple tokentype networks§
    internal fun TokenParty.network() : RPCConnectionId
            = (this + CordaRPCNetwork.GBP.toString())
        .also { idExists(it) } // check validity

    /**
     * Returns the ArtworkState associated with the ArtworkId
     */
    internal fun ArtworkParty.artworkIdToState(artworkId: ArtworkId): ArtworkState {
        logger.info("Fetching ArtworkState for artworkId $artworkId")
        return network().startFlow(FindArtworkFlow::class.java, artworkId)
    }

    /**
     * Returns the ArtworkState associated with the CordaReference
     */
    internal fun ArtworkParty.artworkIdToCordaReference(artworkId: ArtworkId): CordaReference {
        return artworkIdToState(artworkId).linearId.id
    }
}