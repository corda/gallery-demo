package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct


@Component
class TokenNetworkSellerClientImpl : TokenNetworkSellerClient {

    private lateinit var tokenNetworkSellerCS: ConnectionService

    @Autowired
    @Qualifier("TokenNetworkSellerProperties")
    private lateinit var tokenNetworkSellerProperties: ClientProperties

    // init client and set associated network
    @PostConstruct
    private fun postConstruct() {
        tokenNetworkSellerCS = ConnectionServiceImpl(tokenNetworkSellerProperties)
        tokenNetworkSellerCS.associatedNetwork = network
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkSellerClientImpl::class.java)
        private val network = CordaRPCNetwork.AUCTION
    }

    override fun claimTokens(sellerParty: TokenParty, encumberedTokens: EncumberedTokens, proofOfTransfer: ProofOfTransferOfOwnership): CordaReference {
        TODO("Not yet implemented")
    }

    override fun releaseTokens(sellerParty: TokenParty, buyer: TokenParty, encumberedTokens: EncumberedTokens): CordaReference {
        TODO("Not yet implemented")
    }

}