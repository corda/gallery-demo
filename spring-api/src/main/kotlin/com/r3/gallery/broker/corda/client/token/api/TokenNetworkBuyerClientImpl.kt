package com.r3.gallery.broker.corda.client.token.api

import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import com.r3.gallery.workflows.OfferEncumberedTokensFlow
import com.r3.gallery.workflows.token.IssueTokensFlow
import net.corda.core.contracts.Amount
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
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

    override fun issueTokens(buyer: TokenParty, amount: Long, currency: String) {
        logger.info("Starting IssueTokensFlow via $buyer for $amount $currency")
        //val buyerParty = tokenNetworkBuyerCS.wellKnownPartyFromName(buyer, buyer)
        val signedTx = tokenNetworkBuyerCS.startFlow(buyer, IssueTokensFlow::class.java, amount, currency, buyer)
    }

    override fun transferEncumberedTokens(
        buyer: TokenParty,
        seller: TokenParty,
        amount: Int,
        lockedOn: UnsignedArtworkTransferTx
    ): EncumberedTokens {
        logger.info("Starting OfferEncumberedTokensFlow flow via $buyer with seller: $seller")
        val sellerParty = tokenNetworkBuyerCS.wellKnownPartyFromName(buyer, seller)
        val encumberedAmount = Amount(amount.toLong(), FiatCurrency.getInstance("GBP"))
        val wireTx = SerializedBytes<WireTransaction>(lockedOn.transactionBytes).deserialize()
        val lockStateRef = tokenNetworkBuyerCS.startFlow(buyer, OfferEncumberedTokensFlow::class.java, wireTx, sellerParty, encumberedAmount)
        return LockStateRef(lockStateRef.serialize().bytes)
    }
}