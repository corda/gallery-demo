package com.r3.gallery.broker.corda.client.art.api

import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.TokenParty
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.workflows.token.IssueTokensFlow
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
        val signedTx = artNetworkBidderCS.startFlow(bidderParty, IssueTokensFlow::class.java, amount, currency, bidderParty)
    }
}