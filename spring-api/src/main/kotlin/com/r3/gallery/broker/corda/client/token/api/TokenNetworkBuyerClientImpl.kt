package com.r3.gallery.broker.corda.client.token.api

import com.r3.gallery.api.CordaRPCNetwork
import com.r3.gallery.api.EncumberedTokens
import com.r3.gallery.api.TokenParty
import com.r3.gallery.api.UnsignedArtworkTransferTx
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class TokenNetworkBuyerClientImpl : TokenNetworkBuyerClient {

    private lateinit var tokenNetworkBuyerCS: ConnectionService

    @Autowired
    @Qualifier("TokenNetworkBuyerProperties")
    private lateinit var tokenNetworkBuyerProperties: ClientProperties

    // init client and set associated network
    @PostConstruct
    private fun postConstruct() {
        tokenNetworkBuyerCS = ConnectionServiceImpl(tokenNetworkBuyerProperties)
        tokenNetworkBuyerCS.associatedNetwork = network
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkBuyerClientImpl::class.java)
        private val network = CordaRPCNetwork.AUCTION
    }

    override fun transferEncumberedTokens(buyer: TokenParty, seller: TokenParty, amount: Int, lockedOn: UnsignedArtworkTransferTx): EncumberedTokens {
        TODO("Not yet implemented")
    }

}