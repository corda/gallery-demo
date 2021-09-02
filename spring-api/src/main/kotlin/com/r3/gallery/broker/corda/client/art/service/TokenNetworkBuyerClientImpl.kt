package com.r3.gallery.broker.corda.client.art.service

import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.client.config.ClientProperties
import com.r3.gallery.broker.corda.client.token.api.TokenNetworkBuyerClient
import com.r3.gallery.workflows.OfferEncumberedTokensFlow
import net.corda.core.contracts.Amount
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class TokenNetworkBuyerClientImpl(
    @Autowired
    @Qualifier("TokenNetworkBidderProperties")
    clientProperties: ClientProperties
) : NodeClient(clientProperties), TokenNetworkBuyerClient {

    companion object {
        private val logger = LoggerFactory.getLogger(TokenNetworkBuyerClientImpl::class.java)
    }

    override suspend fun transferEncumberedTokens(
        buyer: TokenParty,
        seller: TokenParty,
        amount: Int,
        lockedOn: UnsignedArtworkTransferTx
    ): EncumberedTokens {
        logger.info("Starting OfferEncumberedTokensFlow flow via $buyer with seller: $seller")
        val sellerParty = buyer.network().wellKnownPartyFromName(buyer)
        val encumberedAmount = Amount(amount.toLong(), FiatCurrency.getInstance("GBP"))
        val wireTx = SerializedBytes<WireTransaction>(lockedOn.transactionBytes).deserialize()
        val lockStateRef = buyer.network().startFlow(OfferEncumberedTokensFlow::class.java, wireTx, sellerParty, encumberedAmount)
        return LockStateRef(lockStateRef.serialize().bytes)
    }

    // TODO: review for multiple tokentype networks§
    internal fun TokenParty.network() : RPCConnectionId
            = (this + CordaRPCNetwork.GBP.toString())
        .also { idExists(it) } // check validity
}